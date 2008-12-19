importClass(Packages.ij3d.Image3DUniverse);

importClass(Packages.java.util.ArrayList);

importClass(Packages.javax.vecmath.Color3f);
importClass(Packages.javax.vecmath.Point3f);

function addRectangle(array, x1, y1, z1, x2, y2, z2) {
	var x3, y3, z3, x4, y4, z4;
	if (x1 == x2) {
		x3 = x4 = x1;
		y3 = y1; y4 = y2;
		z3 = z2; z4 = z1;
	} else if (y1 == y2) {
		x3 = x1; x4 = x2;
		y3 = y4 = y1;
		z3 = z2; z4 = z1;
	} else {
		x3 = x1; x4 = x2;
		y3 = y2; y4 = y1;
		z3 = z4 = z1;
	}
	array.add(new Point3f(x1, y1, z1));
	array.add(new Point3f(x2, y2, z2));
	array.add(new Point3f(x3, y3, z3));
	array.add(new Point3f(x1, y1, z1));
	array.add(new Point3f(x4, y4, z4));
	array.add(new Point3f(x2, y2, z2));
}

function addCuboid(array, x1, y1, z1, x2, y2, z2) {
	addRectangle(array, x1, y1, z2, x2, y2, z2);
	addRectangle(array, x2, y1, z1, x2, y2, z2);
	addRectangle(array, x2, y1, z1, x1, y2, z1);
	addRectangle(array, x1, y2, z1, x1, y1, z2);
	addRectangle(array, x1, y1, z1, x2, y1, z2);
	addRectangle(array, x2, y2, z1, x1, y2, z2);
}

var mesh = new ArrayList();
var blue = new Color3f(101 / 255.0, 164 / 255.0, 227 / 255.0);

// 'F'
addCuboid(mesh, 0, 0, 0, 1, 5, 1);
addCuboid(mesh, 0, 5, 0, 7, 6, 1);

// left 'i'
addCuboid(mesh, 2, 2, 0, 3, 4, 1);

// 'j'
addCuboid(mesh, 2, 0, 0, 5, 1, 1);
addCuboid(mesh, 4, 1, 0, 5, 4, 1);

// right 'i'
addCuboid(mesh, 6, 2, 0, 7, 4, 1);

var universe = new Image3DUniverse();
universe.addMesh(mesh, blue, "Fiji", 0);
universe.show();
