package org.helioviewer.jhv.renderable.gui;

import java.util.ArrayList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;

import com.jogamp.opengl.GL2;

public class RenderableContainer implements TableModel, Reorderable {

    private final ArrayList<Renderable> renderables = new ArrayList<Renderable>();
    private final ArrayList<Renderable> newRenderables = new ArrayList<Renderable>();
    private final ArrayList<Renderable> removedRenderables = new ArrayList<Renderable>();
    private final ArrayList<TableModelListener> listeners = new ArrayList<TableModelListener>();

    public void addBeforeRenderable(Renderable renderable) {
        int lastImagelayerIndex = -1;
        int size = renderables.size();
        for (int i = 0; i < size; i++) {
            if (renderables.get(i) instanceof RenderableImageLayer) {
                lastImagelayerIndex = i;
            }
        }
        renderables.add(lastImagelayerIndex + 1, renderable);
        newRenderables.add(renderable);
        fireInsert(lastImagelayerIndex + 1);
    }

    public void addRenderable(Renderable renderable) {
        renderables.add(renderable);
        newRenderables.add(renderable);
        fireInsert(renderables.size() - 1);
    }

    public void removeRenderable(Renderable renderable) {
        renderables.remove(renderable);
        removedRenderables.add(renderable);
        // fireListeners();
        Displayer.display();
    }

    public void prerender(GL2 gl) {
        int count = removeRenderables(gl);
        initRenderables(gl);
        if (count > 0)
            fireListeners();

        for (Renderable renderable : renderables) {
            renderable.prerender(gl);
        }
    }

    public void render(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.render(camera, vp, gl);
        }
    }

    public void renderLatitudinal(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.renderLatitudinal(camera, vp, gl);
        }
    }

    public void renderPolar(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.renderPolar(camera, vp, gl);
        }
    }

    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.renderFloat(camera, vp, gl);
        }
    }

    public void renderFullFloat(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.renderFullFloat(camera, vp, gl);
        }
    }

    public void renderMiniview(Camera camera, Viewport miniview, GL2 gl) {
        ImageViewerGui.getRenderableMiniview().renderBackground(camera, miniview, gl);
        for (Renderable renderable : renderables) {
            renderable.renderMiniview(camera, miniview, gl);
        }
    }

    private void initRenderables(GL2 gl) {
        for (Renderable renderable : newRenderables) {
            renderable.init(gl);
        }
        newRenderables.clear();
    }

    private int removeRenderables(GL2 gl) {
        int count = removedRenderables.size();
        for (Renderable renderable : removedRenderables) {
            renderable.remove(gl);
        }
        removedRenderables.clear();
        return count;
    }

    private void insertRow(int row, Renderable rowData) {
        if (row > renderables.size()) {
            renderables.add(rowData);
        } else {
            renderables.add(row, rowData);
        }
    }

    @Override
    public void reorder(int fromIndex, int toIndex) {
        if (toIndex > renderables.size()) {
            return;
        }
        Renderable toMove = renderables.get(fromIndex);
        Renderable moveTo = renderables.get(Math.max(0, toIndex - 1));

        if (!(toMove instanceof RenderableImageLayer) || !(moveTo instanceof RenderableImageLayer)) {
            return;
        }
        renderables.remove(fromIndex);
        if (fromIndex < toIndex) {
            insertRow(toIndex - 1, toMove);
        } else {
            insertRow(toIndex, toMove);
        }
        fireListeners();

        if (Displayer.multiview) {
            arrangeMultiView(true);
        }
    }

    @Override
    public int getRowCount() {
        return renderables.size();
    }

    @Override
    public int getColumnCount() {
        return RenderableContainerPanel.NUMBER_COLUMNS;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return String.valueOf(columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Renderable.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return renderables.get(rowIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        renderables.add(rowIndex, (Renderable) aValue);
        fireListeners();
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    public void removeRow(int row) {
        removeRenderable(renderables.get(row));
    }

    public void fireInsert(int idx) {
        TableModelEvent e = new TableModelEvent(this, idx, idx, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
        for (TableModelListener listener : listeners) {
            listener.tableChanged(e);
        }
    }

    public void fireListeners() {
        TableModelEvent e = new TableModelEvent(this);
        for (TableModelListener listener : listeners) {
            listener.tableChanged(e);
        }
    }

    public void fireTimeUpdated(Renderable renderable) {
        int idx = renderables.indexOf(renderable);
        if (idx < 0 || idx >= renderables.size())
            return;

        TableModelEvent e = new TableModelEvent(this, idx, idx, RenderableContainerPanel.TIME_COL, TableModelEvent.UPDATE);
        for (TableModelListener listener : listeners) {
            listener.tableChanged(e);
        }
    }

    public int getRowIndex(Renderable renderable) {
        return renderables.indexOf(renderable);
    }

    public void init(GL2 gl) {
        GLText.init(gl);
        for (Renderable renderable : renderables) {
            renderable.init(gl);
        }
    }

    public void dispose(GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.dispose(gl);
        }
        GLText.dispose(gl);
    }

    public RenderableImageLayer getViewportRenderableImageLayer(int idx) {
        for (Renderable r : renderables) {
            if (r instanceof RenderableImageLayer && r.isVisible(idx)) {
                return (RenderableImageLayer) r;
            }
        }
        return null;
    }

    public void setRender(Camera camera, double factor) {
        for (Renderable r : renderables) {
            int i;
            if (r instanceof RenderableImageLayer && (i = r.isVisibleIdx()) != -1) {
                RenderableImageLayer im = (RenderableImageLayer) r;
                im.setRender(camera, Displayer.getViewports()[i], factor);
            }
        }
    }

    public void arrangeMultiView(boolean multiview) {
        int ctImages = 0;

        if (multiview) {
            for (Renderable r : renderables) {
                if (r instanceof RenderableImageLayer && r.isVisible()) {
                    RenderableImageLayer im = (RenderableImageLayer) r;
                    r.setVisible(ctImages);
                    im.getGLImage().setOpacity(1);
                    ctImages++;
                }
            }
        } else {
            for (Renderable r : renderables) {
                if (r instanceof RenderableImageLayer && r.isVisible()) {
                    RenderableImageLayer im = (RenderableImageLayer) r;
                    r.setVisible(0);
                    float opacity;
                    if (im.getName().contains("LASCO") || im.getName().contains("COR"))
                        opacity = 1;
                    else {
                        opacity = (float) (1. / (1. + ctImages));
                        ctImages++;
                    }
                    im.getGLImage().setOpacity(opacity);
                }
            }
        }
        Displayer.reshapeAll();
        Displayer.render(1);
    }

}
