#! /usr/bin/env python

from math import pi, sin, cos
from base_spiral import BaseSpiral, run

# =============================================================================
class ArchimedeanSpiral(BaseSpiral):

	def __init__(self, segment_count, **kwargs):

		BaseSpiral.__init__(self, segment_count)

	# ---------------------------------------------------------------------
	def spiralLength(self, w):
		from math import log, sqrt
		return ( (w * sqrt(1 + w**2)) + log(w + sqrt(1 + w**2)) ) / 2

	# ---------------------------------------------------------------------
	def spiralSlope(self, w):
		dy = sin(w)/(2*pi) + self.spiralRadius(w)*cos(w)
		dx = cos(w)/(2*pi) - self.spiralRadius(w)*sin(w)
		return dy / dx

	# ---------------------------------------------------------------------
	def spiralRadius(self, theta):
		return theta/(2*pi)

# =============================================================================
if __name__ == "__main__":
	run(ArchimedeanSpiral)
