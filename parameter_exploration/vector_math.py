def vector_scale(vector, scalar):
	return map(lambda a: a*scalar, vector)

# -------------

def vector_add(first_point, second_point):
	return map(lambda x,y: x+y, first_point, second_point)

# -------------

def displacement(start_point, end_point):
	return map(lambda x,y: y-x, start_point, end_point)

# -------------

def dot_product(first_point, second_point):
	return sum(map(lambda x,y: x*y, first_point, second_point))

# -------------

def cross_product_3D(a, b):
	return [a[1]*b[2] - a[2]*b[1], a[2]*b[0] - a[0]*b[2], a[0]*b[1] - a[1]*b[0]]

# -------------

def magnitude(vector):
	from math import sqrt
	return sqrt( sum( map(lambda w: w*w, vector) ) )

# -------------

def midpoint(first_point, second_point):
	return vector_scale( vector_add(first_point, second_point), 0.5 )

# -------------

def distance(start_point, end_point):
	return magnitude(displacement(start_point, end_point))

# -------------

def interpolate(start_point, end_point, fraction):
	resultant = vector_add(start_point, vector_scale(displacement(start_point, end_point), fraction) )
	return resultant

# -------------

def normalize(vector):
	return vector_scale( vector, 1/magnitude(vector) )

