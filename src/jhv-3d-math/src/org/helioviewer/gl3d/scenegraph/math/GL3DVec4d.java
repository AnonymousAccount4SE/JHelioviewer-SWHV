package org.helioviewer.gl3d.scenegraph.math;

public class GL3DVec4d {
    /**
     * Predefined Vectors
     */

    /**
     * Coordinates
     */
    public double x;
    public double y;
    public double z;
    public double w;

    // Constructors

    public GL3DVec4d(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public GL3DVec4d(GL3DVec4d vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = vector.z;
        this.w = vector.w;
    }
    
	public GL3DVec4d(GL3DVec3d vector) {
		this.x = vector.x;
		this.y = vector.y;
		this.z = vector.z;
		this.w = 0.0;
	}

    public GL3DVec4d() {
        this(0f, 0f, 0f, 0f);
    }

    public GL3DVec4d(double[] coordinates) {
        if (coordinates == null || coordinates.length < 4) {
            throw new IllegalArgumentException("Coordinate Array must contain at least 4 dimensions");
        }
        this.x = coordinates[0];
        this.y = coordinates[1];
        this.z = coordinates[2];
        this.w = coordinates[3];
    }

    public GL3DVec4d add(GL3DVec4d vec) {
        this.x += vec.x;
        this.y += vec.y;
        this.z += vec.z;
        this.w += vec.w;
        return this;
    }

    public GL3DVec4d add(double s) {
        this.x += s;
        this.y += s;
        this.z += s;
        this.w += w;
        return this;
    }

    public static GL3DVec4d add(GL3DVec4d vec1, GL3DVec4d vec2) {
        return new GL3DVec4d(vec1.x + vec2.x, vec1.y + vec2.y, vec1.z + vec2.z, vec1.w + vec2.w);
    }

    public static GL3DVec4d add(GL3DVec4d vec1, double s) {
        return new GL3DVec4d(vec1.x + s, vec1.y + s, vec1.z + s, vec1.w + s);
    }

    public GL3DVec4d subtract(GL3DVec4d vec) {
        this.x -= vec.x;
        this.y -= vec.y;
        this.z -= vec.z;
        this.w -= vec.w;
        return this;
    }

    public GL3DVec4d subtract(double s) {
        this.x -= s;
        this.y -= s;
        this.z -= s;
        this.w -= s;
        return this;
    }

    public static GL3DVec4d subtract(GL3DVec4d vec1, GL3DVec4d vec2) {
        return new GL3DVec4d(vec1.x - vec2.x, vec1.y - vec2.y, vec1.z - vec2.z, vec1.w - vec2.w);
    }

    public static GL3DVec4d subtract(GL3DVec4d vec1, double s) {
        return new GL3DVec4d(vec1.x - s, vec1.y - s, vec1.z - s, vec1.w - s);
    }

    public GL3DVec4d divide(GL3DVec4d vec) {
        if (vec.x == 0f || vec.y == 0f || vec.z == 0f || vec.w == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= vec.x;
        this.y /= vec.y;
        this.z /= vec.z;
        this.w /= vec.w;
        return this;
    }

    public GL3DVec4d divide(double s) {
        if (s == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        this.x /= s;
        this.y /= s;
        this.z /= s;
        this.w /= s;
        return this;
    }

    public static GL3DVec4d divide(GL3DVec4d vec1, GL3DVec4d vec2) {
        if (vec2.x == 0f || vec2.y == 0f || vec2.z == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new GL3DVec4d(vec1.x / vec2.x, vec1.y / vec2.y, vec1.z / vec2.z, vec1.w / vec2.w);
    }

    public static GL3DVec4d divide(GL3DVec4d vec1, double s) {
        if (s == 0f)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new GL3DVec4d(vec1.x / s, vec1.y / s, vec1.z / s, vec1.w / s);
    }

    public GL3DVec4d multiply(GL3DVec4d vec) {
        this.x *= vec.x;
        this.y *= vec.y;
        this.z *= vec.z;
        this.w *= vec.w;
        return this;
    }

    public GL3DVec4d multiply(double s) {
        this.x *= s;
        this.y *= s;
        this.z *= s;
        this.w *= s;
        return this;
    }

    public static GL3DVec4d multiply(GL3DVec4d vec1, GL3DVec4d vec2) {
        return new GL3DVec4d(vec1.x * vec2.x, vec1.y * vec2.y, vec1.z * vec2.z, vec1.w * vec2.w);
    }

    public static GL3DVec4d multiply(GL3DVec4d vec1, double s) {
        return new GL3DVec4d(vec1.x * s, vec1.y * s, vec1.z * s, vec1.w * s);
    }

    public double dot(GL3DVec4d vec) {
        return GL3DVec4d.dot(this, vec);
    }

    public static double dot(GL3DVec4d u, GL3DVec4d v) {
        return (u.x * v.x) + (u.y * v.y) + (u.z * v.z) + (u.w * v.w);
    }

    public GL3DVec4d cross(GL3DVec4d vec) {
        return GL3DVec4d.cross(this, vec);
    }

    public static GL3DVec4d cross(GL3DVec4d u, GL3DVec4d v) {
        return new GL3DVec4d(u.y * v.z - u.z * v.y, u.z * v.x - u.x * v.z, u.x * v.y - u.y * v.x, 1f);
    }

    public GL3DVec4d negate() {
        return this.multiply(-1f);
    }

    public static GL3DVec4d negate(GL3DVec4d vec) {
        return vec.multiply(-1f);
    }

    public boolean isApproxEqual(GL3DVec4d vec, double tolerance) {
        return Math.abs(this.x - vec.x) <= tolerance && Math.abs(this.y - vec.y) <= tolerance && Math.abs(this.z - vec.z) <= tolerance && Math.abs(this.w - vec.w) <= tolerance;
    }

    public double length() {
        return (double) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);
    }

    public double length2() {
        return (this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);
    }

    public GL3DVec4d normalize() {
        return this.divide(length());
    }

    public double[] toArray() {
        return new double[] { x, y, z, w };
    }

    public boolean equals(Object o) {
        if (o instanceof GL3DVec4d)
            return isApproxEqual((GL3DVec4d) o, 0.0f);
        return false;
    }

    public Object clone() {
        return new GL3DVec4d(this);
    }

    public static double[] toArray(GL3DVec4d[] vecs) {
        double[] arr = new double[vecs.length * 4];
        for (int i = 0; i < vecs.length; i++) {
            GL3DVec4d v = vecs[i];
            arr[i * 4 + 0] = v.x;
            arr[i * 4 + 1] = v.y;
            arr[i * 4 + 2] = v.z;
            arr[i * 4 + 3] = v.w;
        }
        return arr;
    }

    public String toString() {
        return "[" + x + ", " + y + ", " + z + ", " + w + "]";
    }

}
