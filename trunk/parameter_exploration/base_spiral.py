#! /usr/bin/env python
import pygtk
pygtk.require('2.0')
import gtk, gobject, cairo


from math import sin, cos, exp
from math import sqrt, pi
from time import time
# =============================================================================
class BezierPoint:
	def __init__(self, start, c0, c1, end):
		self.start = start
		self.c0 = c0
		self.c1 = c1
		self.end = end

	def getBezierArgs(self):
		return self.c0 + self.c1 + self.end

	def getReverseBezierArgs(self):
		return self.c1 + self.c0 + self.start

# =============================================================================
class BaseSpiral(gtk.DrawingArea):

	'''The spiral grows outward with increasing angle in a counter-clockwise direction.'''

	# Draw in response to an expose-event
	__gsignals__ = { "expose-event": "override" }

	# ---------------------------------------------------------------------
	# Handle the expose-event by drawing
	def do_expose_event(self, event):

		# Create the cairo context
		cr = self.window.cairo_create()

		# Restrict Cairo to the exposed area; avoid extra work
		cr.rectangle(event.area.x, event.area.y, event.area.width, event.area.height)
		cr.clip()

		self.draw(cr, *self.window.get_size())

		if not self.animation_button.get_active():
			self.queue_draw()

	# ---------------------------------------------------------------------
	def __init__(self, segment_count, **kwargs):
		gtk.DrawingArea.__init__(self, *kwargs)

		self.last_timestamp = time()
		self.last_angle = 0
		self.scale = 1

		self.pitch = 1/(2*pi)
		self.doubled = False
		self.quadrupled = False
		self.periods = 8
		self.speed = 10	# Seconds per rotation

		self.reinitialize(segment_count)

	# ---------------------------------------------------------------------
	def reinitialize(self, segment_count):
		self.segment_count = segment_count
		self.incremental_angle = 2*pi/self.segment_count

		self.iteration_count = self.periods*self.segment_count

		self.bezier_points = self.generateBezierPoints()

	# ---------------------------------------------------------------------
	def generateBezierPoints(self):
		points = []
		for i in range(self.iteration_count):

			iteration_angle = i * self.incremental_angle
			points.append( self.getBezierPoint(iteration_angle) )

		return points

	# ---------------------------------------------------------------------
	def getBezierPoint(self, w):
		x, y = self.solveBezierControlPoints(self.incremental_angle, w)
		return BezierPoint( *zip(x, y) )

	# ---------------------------------------------------------------------
	def spinner_changed(self, widget):
		segment_count = int(widget.get_value())
		self.reinitialize(segment_count)
		self.queue_draw()

	# ---------------------------------------------------------------------
	# Returns the coefficients for P[0], P[1], P[2], P[3]
	def cubicBezierCoefficients(self, t):
		return [(1-t)**3, 3*((1-t)**2)*t, 3*(1-t)*(t**2), t**3]

	# ---------------------------------------------------------------------
	def spiralLength(self, w):
		from math import sqrt
		return sqrt(1 + self.pitch**2) * self.spiralRadius(w) / self.pitch

	# ---------------------------------------------------------------------
	def spiralSlope(self, w):
		dy = self.spiralRadius(w)*sin(w)*self.pitch + self.spiralRadius(w)*cos(w)
		dx = self.spiralRadius(w)*cos(w)*self.pitch - self.spiralRadius(w)*sin(w)
		return dy / dx

	# ---------------------------------------------------------------------
	def solveBezierControlPoints(self, theta, curve_start_angle):

		curve_end_angle = curve_start_angle + theta
		curve_mid_angle = (curve_start_angle + curve_end_angle)/2

		curve_start_point = self.spiralPoint(curve_start_angle)
		curve_end_point = self.spiralPoint(curve_end_angle)
		curve_mid_point = self.spiralPoint(curve_mid_angle)

		midtime = (self.spiralLength(curve_end_angle) - self.spiralLength(curve_mid_angle)) / (self.spiralLength(curve_end_angle) - self.spiralLength(curve_start_angle))
		mc = self.cubicBezierCoefficients(midtime)	# midpoint coefficients


		s0 = self.spiralSlope(curve_start_angle)	# The slope at the start of the curve
		s1 = self.spiralSlope(curve_end_angle)	# The slope at the end of the curve

		# Create an 8x8 matrix with a 8x1 solution column as follows:
		# eq0: [x0, x1, x2, x3, y0, y1, y2, y3] = [s0]
		#                      .                   .
		#                      .                   .
		#                      .                   .
		# eq7: [x0, x1, x2, x3, y0, y1, y2, y3] = [s7]

		from scipy import matrix, transpose
		A = matrix([
			[1] + [0]*7,	# X and Y coordinates of endpoints
			[0]*4 + [1] + [0]*3,
			[0]*3 + [1] + [0]*4,
			[0]*7 + [1],
			mc + [0]*4,	# Midpoint x-coefficients
			[0]*4 + mc,	# Midpoint y-coefficients
			[s0, -s0, 0, 0, -1, 1, 0, 0],	# Slope at start point
			[0, 0, -s1, s1, 0, 0, 1, -1],	# Slope at end point
		])

		b = matrix(map (lambda x: [x], curve_start_point + curve_end_point + curve_mid_point + tuple([0]*2)))

#		from scipy.linalg import solve
#		solution = solve(A,b)
#		sol = transpose(solution)[0].tolist()
		solution =  A.I*b
		sol = transpose(solution)[0].tolist()[0]

		return (sol[:4], sol[4:])

	# ---------------------------------------------------------------------
	def spiralRadius(self, theta):
		return exp(self.pitch*theta)

	# ---------------------------------------------------------------------
	def spiralPoint(self, theta):
		r = self.spiralRadius(theta)
		return self.polar2rect(r, theta)

	# ---------------------------------------------------------------------
	def uniformScale(self, cr, scale):
		cr.scale(scale, scale)

	# ---------------------------------------------------------------------
	def draw(self, cr, width, height):

		cr.set_line_cap(cairo.LINE_CAP_ROUND)

		half_width = width/2.0

		cr.translate(half_width, height/2.0)	# Center within the canvas
		cr.scale(1, -1)	# Invert the y-axis

		self.control_point_radius = half_width/100
		self.end_point_radius = 2*self.control_point_radius
		self.R = half_width * self.scale
		self.uniformScale(cr, self.R)

		last_time = self.last_timestamp
		self.last_timestamp = time()

		prev_angle = self.last_angle
		rotation_quantity = (self.last_timestamp - last_time) * 2*pi/self.speed

		self.last_angle = (prev_angle + rotation_quantity) % 360

		cr.rotate(self.last_angle)

		
		self.drawSpiral(cr)
#		self.drawControlPoints(cr)
#		self.drawNaturalEndPoints(cr)

	# ---------------------------------------------------------------------
	def polar2rect(self, radius, theta):
		return (radius*cos(theta), radius*sin(theta))

	# ---------------------------------------------------------------------
	def drawQuarterSpiralArm(self, cr):
		cr.move_to(*self.bezier_points[0].start)
		for bp in self.bezier_points:
			cr.curve_to( *bp.getBezierArgs() )

		last_bp = self.bezier_points[-1]
		if self.pitch > 0:
			cr.line_to(last_bp.end[0], last_bp.end[0])
		cr.line_to(last_bp.end[1], last_bp.end[0])


		cr.save()
		cr.rotate(pi/2)

		self.bezier_points.reverse()
		cr.line_to(*self.bezier_points[0].end)
		for bp in self.bezier_points:
			cr.curve_to( *bp.getReverseBezierArgs() )

		cr.restore()

		if self.pitch < 0:
			cr.line_to( 1, 1 )



		cr.close_path()
		cr.fill()

		self.bezier_points.reverse()	# Restore original ordering


	# ---------------------------------------------------------------------
	def drawHalfSpiralArm(self, cr):

		cr.move_to(*self.bezier_points[0].start)
		for bp in self.bezier_points:
			cr.curve_to( *bp.getBezierArgs() )


		if self.pitch > 0:
			last_bp = self.bezier_points[-1]
			cr.line_to(last_bp.end[0], last_bp.end[0])
			cr.line_to(-last_bp.end[0], last_bp.end[0])
			cr.line_to(-last_bp.end[0], last_bp.end[1])

		cr.rotate(pi)
		self.bezier_points.reverse()
		cr.line_to(*self.bezier_points[0].end)
		for bp in self.bezier_points:
			cr.curve_to( *bp.getReverseBezierArgs() )


		if self.pitch < 0:
			cr.rotate(pi/2)
			cr.line_to( -1, -1 )
			cr.line_to( 1, -1 )
			cr.line_to( 1, 1 )


		cr.close_path()
		cr.fill()

		self.bezier_points.reverse()	# Restore original ordering

	# ---------------------------------------------------------------------
	def drawSpiral(self, cr):

		cr.set_source_rgb(0.5, 0.5, 0.9)

		if self.quadrupled:

			cr.set_source_rgb(0.5, 0.5, 0.9)
			self.drawQuarterSpiralArm(cr)

			cr.rotate(pi)

			cr.set_source_rgb(0.5, 0.9, 0.5)
			self.drawQuarterSpiralArm(cr)

		elif self.doubled:

			self.drawHalfSpiralArm(cr)

		else:
			cr.set_line_width(10 / self.R)

			# Draw the spiral
			cr.move_to(*self.bezier_points[0].start)
			for bp in self.bezier_points:
				cr.curve_to( *bp.getBezierArgs() )

			cr.stroke()

	# ---------------------------------------------------------------------
	def drawNaturalEndPoints(self, cr):
		# Draw spiral point directly from the formula
		cr.set_source_rgb(1, 0, 0)
		for i in range(self.iteration_count):

			from vector_math import magnitude
			spiral_point = self.spiralPoint(i * self.incremental_angle)

			self.drawCircle(cr, 0.5*self.end_point_radius / self.R, spiral_point)
			cr.stroke()

	# ---------------------------------------------------------------------
	def drawControlPoints(self, cr):

		'''The dotted control point handle uses the "start" Bezier point and the first control point.'''

		from colorsys import hsv_to_rgb

		for i in range(self.iteration_count):
			# Draw control points

			iteration_angle = i * self.incremental_angle
			bp = self.getBezierPoint(iteration_angle)

			cr.set_source_rgb( *hsv_to_rgb(float(i)/self.iteration_count, 0.8, 0.6) )


			line_width = 2 / self.R
			cr.set_line_width(line_width)

			cr.set_dash([3*line_width])

			cr.move_to(*bp.start)
			cr.line_to(*bp.c0)
			cr.stroke()

			self.drawCircle(cr, self.control_point_radius / self.R, bp.c0)
			cr.close_path()
			cr.stroke()

			cr.set_dash([])

			cr.move_to(*bp.end)
			cr.line_to(*bp.c1)
			cr.stroke()

			self.drawCircle(cr, self.control_point_radius / self.R, bp.c1)
			cr.close_path()
			cr.stroke()

	# ---------------------------------------------------------------------
	def drawCircle(self, cr, radius, center=(0,0)):
		cr.arc( center[0], center[1], radius, 0, 2 * pi)
		cr.close_path()

	# ---------------------------------------------------------------------
	def toggle_animation(self, widget):

		if not widget.get_active():
			self.last_timestamp = time()
			self.queue_draw()

	# ---------------------------------------------------------------------
	def pitch_changed(self, widget):
		if widget.get_value():
			self.pitch = 1/(widget.get_value()* 2*pi)
			self.reinitialize(self.segment_count)
			self.queue_draw()

	# ---------------------------------------------------------------------
	def periods_changed(self, widget):
		self.periods = int( widget.get_value() )
		self.reinitialize(self.segment_count)
		self.queue_draw()

	# ---------------------------------------------------------------------
	def scale_changed(self, widget):
		self.scale = pow(2, widget.get_value())
		self.queue_draw()

	# ---------------------------------------------------------------------
	def speed_changed(self, widget):
		self.speed = pow(2, widget.get_value())

	# ---------------------------------------------------------------------
	def doubler_toggled(self, widget):
		self.doubled = widget.get_active()
		self.queue_draw()

	# ---------------------------------------------------------------------
	def quad_toggled(self, widget):
		self.quadrupled = widget.get_active()
		self.queue_draw()


	# ---------------------------------------------------------------------
	def capture_screenshot(self, widget):

		img_width = 512
		img_height = 512
		filename = "spiral"

		raster = True
		svg = True
		pdf = False
		if raster:
			img = cairo.ImageSurface(cairo.FORMAT_ARGB32, img_width, img_height)
			width, height = img.get_width(), img.get_height()
			c = cairo.Context(img)
			self.draw(c, width, height)
			img.write_to_png(filename + ".png")
		if pdf:
			surf = cairo.PDFSurface(filename + ".pdf", img_width, img_height)
			cr = cairo.Context(surf)
			self.draw(cr, img_width, img_height)
			cr.show_page()
		if svg:
			surf = cairo.SVGSurface(filename + ".svg", img_width, img_height)
			cr = cairo.Context(surf)
			self.draw(cr, img_width, img_height)
			cr.show_page()

# =============================================================================
# GTK mumbo-jumbo to show the widget in a window and quit when it's closed
def run(Widget):
	window = gtk.Window()
	window.set_size_request(600, 400)
	window.connect("delete-event", gtk.main_quit)
	running_vbox = gtk.VBox(False, 0)
	window.add(running_vbox)


	running_hbox = gtk.HBox(False, 5)

	widget = Widget(5)

	button = gtk.Button("Screenshot")
	button.connect("clicked", widget.capture_screenshot)
	running_vbox.pack_start(running_hbox, False, False)

	running_hbox.pack_start(button, False, False)

	widget.animation_button = gtk.ToggleButton("Pause")
	widget.animation_button.connect("toggled", widget.toggle_animation)
	running_hbox.pack_start(widget.animation_button, False, False)





	adj = gtk.Adjustment(5, 3, 10, 1, 0, 0)
	spinner2 = gtk.SpinButton(adj, 0.0, 0)
	spinner2.set_wrap(True)
	adj.connect("value_changed", widget.spinner_changed)
	running_hbox.pack_start(spinner2, False, False)


	running_hbox.pack_start(gtk.Label("Pitch:"), False, False)
	hscale = gtk.HScale(gtk.Adjustment(1, -10, 10, 0, 0, 0))
	hscale.set_digits(2)
	hscale.set_value_pos(gtk.POS_RIGHT)
	hscale.connect("value_changed", widget.pitch_changed)
	running_hbox.pack_start(hscale, True, True)



	running_hbox.pack_start(gtk.Label("Scale:"), False, False)
	hscale = gtk.HScale(gtk.Adjustment(0, -10, 10, 0, 0, 0))
	hscale.set_digits(2)
	hscale.set_value_pos(gtk.POS_RIGHT)
	hscale.connect("value_changed", widget.scale_changed)
	hscale.set_value(-5)
	running_hbox.pack_start(hscale, True, True)

	doubler = gtk.CheckButton("Double")
	doubler.connect("toggled", widget.doubler_toggled)
	running_hbox.pack_start(doubler, False, False)

	doubler = gtk.CheckButton("Quad")
	doubler.connect("toggled", widget.quad_toggled)
	running_hbox.pack_start(doubler, False, False)


	running_hbox = gtk.HBox(False, 5)

	running_vbox.pack_start(running_hbox, False, False)
	running_hbox.pack_start(gtk.Label("Periods:"), False, False)
	hscale = gtk.HScale(gtk.Adjustment(10, 1, 250, 1, 0, 0))
	hscale.set_digits(0)
	hscale.set_value_pos(gtk.POS_RIGHT)
	hscale.connect("value_changed", widget.periods_changed)
	running_hbox.pack_start(hscale, True, True)

	running_hbox.pack_start(gtk.Label("Speed:"), False, False)
	hscale = gtk.HScale(gtk.Adjustment(0, -2, 6, 0, 0, 0))
	hscale.set_digits(2)
	hscale.set_value_pos(gtk.POS_RIGHT)
	hscale.connect("value_changed", widget.speed_changed)
	running_hbox.pack_start(hscale, True, True)


	running_vbox.pack_start(widget, True, True)


	window.show_all()
	gtk.main()

# =============================================================================
if __name__ == "__main__":
	run(BaseSpiral)
