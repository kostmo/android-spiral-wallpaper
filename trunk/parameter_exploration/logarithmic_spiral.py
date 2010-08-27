#! /usr/bin/env python

from math import sqrt, pi, sin, cos, exp
from base_spiral import BaseSpiral, run

# =============================================================================
class LogarithmicSpiral(BaseSpiral):

	def __init__(self, segment_count, **kwargs):

		self.pitch = 1/(2*pi)

		BaseSpiral.__init__(self, segment_count)

	# ---------------------------------------------------------------------
	def spiralLength(self, w):
		from math import sqrt
		return sqrt(1 + self.pitch**2) * self.spiralRadius(w) / self.pitch

	# ---------------------------------------------------------------------
	def spiralSlope(self, w):
		dy = sin(w)*self.pitch + cos(w)
		dx = cos(w)*self.pitch - sin(w)
		return dy / dx

	# ---------------------------------------------------------------------
	def spiralRadius(self, theta):
		return exp(self.pitch*theta)

# =============================================================================
if __name__ == "__main__":
	run(LogarithmicSpiral)
