package org.helioviewer.jhv.data.datatype.event;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the relationship between events.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class JHVEventRelationship {

    /** Events following this event */
    private final Map<String, JHVEventRelation> nextEvents;
    /** Events preceding this event */
    private final Map<String, JHVEventRelation> precedingEvents;
    /** Rules for relation with this event */
    private List<JHVEventRelationShipRule> relationshipRules = null;
    /** Related events */
    private final Map<String, JHVEventRelation> relatedEventsByRule;
    /**  */
    private Color relationshipColor;

    /**
     * Default constructor of JHV event relationship.
     */
    public JHVEventRelationship() {
        nextEvents = new HashMap<String, JHVEventRelation>();
        precedingEvents = new HashMap<String, JHVEventRelation>();
        relatedEventsByRule = new HashMap<String, JHVEventRelation>();
        relationshipColor = null;
    }

    /**
     * Gets the related events by rule.
     *
     * @return the relatedEventsByRule
     */
    public Map<String, JHVEventRelation> getRelatedEventsByRule() {
        return relatedEventsByRule;
    }

    /**
     * Gets the next events.
     *
     * @return the nextEvents
     */
    public Map<String, JHVEventRelation> getNextEvents() {
        return nextEvents;
    }

    /**
     * Gets the preceding events.
     *
     * @return the precedingEvents
     */
    public Map<String, JHVEventRelation> getPrecedingEvents() {
        return precedingEvents;
    }

    public List<JHVEventRelationShipRule> getRelationshipRules() {
        return relationshipRules;
    }

    public void setRelationshipRules(List<JHVEventRelationShipRule> relationshipRules) {
        this.relationshipRules = relationshipRules;
    }

    public Color getRelationshipColor() {
        return relationshipColor;
    }

    public void setRelationshipColor(Color color) {
        relationshipColor = color;
    }

    public void merge(JHVEventRelationship eventRelationShip) {
        Map<String, JHVEventRelation> otherNextEvents = eventRelationShip.getNextEvents();
        Map<String, JHVEventRelation> otherPrecedingEvents = eventRelationShip.getPrecedingEvents();
        Map<String, JHVEventRelation> otherRulesRelated = eventRelationShip.getRelatedEventsByRule();
        mergeTwoLists(nextEvents, otherNextEvents);
        mergeTwoLists(precedingEvents, otherPrecedingEvents);
        mergeTwoLists(relatedEventsByRule, otherRulesRelated);
    }

    private void mergeTwoLists(Map<String, JHVEventRelation> currentList, Map<String, JHVEventRelation> newList) {
        for (Map.Entry<String, JHVEventRelation> entry : newList.entrySet()) {
            String identifier = entry.getKey();
            if (!currentList.containsKey(identifier)) {
                JHVEventRelation newRelatedEvent = entry.getValue();
                currentList.put(identifier, newRelatedEvent);

                if (newRelatedEvent.getTheEvent() != null) {
                    newRelatedEvent.getTheEvent().getEventRelationShip().setRelationshipColor(relationshipColor);
                }
            }
        }
    }

}
