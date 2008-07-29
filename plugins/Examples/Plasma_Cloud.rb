$w = 400
$h = 300

cp = ij.process.ColorProcessor.new($w,$h)
$i = ij.ImagePlus.new "Plasma Cloud", cp

pixels = cp.getPixels

$rng = java.util.Random.new( Time.now.to_i )

$last_time_displayed = java.lang.System.currentTimeMillis
$update_every = 500

def fill_in_rectangle( pixels, x_min, x_max, y_min, y_max )
  # Don't redraw all the time, only every $update_every milliseconds
  now = java.lang.System.currentTimeMillis
  if (now - $last_time_displayed) > $update_every
    $last_time_displayed = now
    $i.updateAndDraw
  end
  # Stop recursing if there's nothing in this rectangle to fill in:
  return if (x_max - x_min <= 1) && (y_max - y_min <= 1)
  x_mid = (x_min + x_max) / 2
  y_mid = (y_min + y_max) / 2
  # Do the points in the middle of the top row and the bottom row:
  if x_mid != x_min
    pixels[ to_index( x_mid, y_min ) ] =
      color_between( x_max - x_mid,
                     [ pixels[ to_index( x_min, y_min ) ],
                       pixels[ to_index( x_max, y_min ) ] ] )
    pixels[ to_index( x_mid, y_max ) ] =
      color_between( x_max - x_mid,
                     [ pixels[ to_index( x_min, y_max ) ],
                       pixels[ to_index( x_max, y_max ) ] ] )
  end
  # Do the points in the middle of the left colum and the right column:
  if y_mid != y_min
    pixels[ to_index( x_min, y_mid ) ] =
      color_between( y_max - y_mid,
                     [ pixels[ to_index( x_min, y_min ) ],
                       pixels[ to_index( x_min, y_max ) ] ] )      
    pixels[ to_index( x_max, y_mid ) ] =
      color_between( y_max - y_mid,
                     [ pixels[ to_index( x_max, y_min ) ],
                       pixels[ to_index( x_max, y_max ) ] ] )
  end
  # Now the middle point:
  pixels[ to_index( x_mid, y_mid ) ] =
    color_between( [ x_max - x_mid, y_max - y_mid ].min,
                   [ pixels[ to_index( x_min, y_min ) ],
                     pixels[ to_index( x_max, y_min ) ],
                     pixels[ to_index( x_min, y_max ) ],
                     pixels[ to_index( x_max, y_max ) ] ] )
  # Now recurse onto each of the 4 sub rectangles:
  fill_in_rectangle( pixels, x_min, x_mid, y_min, y_mid )
  fill_in_rectangle( pixels, x_mid, x_max, y_min, y_mid )
  fill_in_rectangle( pixels, x_min, x_mid, y_mid, y_max )
  fill_in_rectangle( pixels, x_mid, x_max, y_mid, y_max )
end

def random_color
  r = $rng.nextInt 256
  g = $rng.nextInt 256
  b = $rng.nextInt 256
  b + (g << 8) + (r << 16)
end

def color_between( separation, colors )
  separation = 1 if separation < 1
  sum_red = sum_green = sum_blue = n = 0
  colors.each do |c|
    n += 1
    sum_blue  += c & 0xFF
    sum_green += (c >> 8)  & 0xFF;
    sum_red   += (c >> 16) & 0xFF;
  end
  # The basic value is the mean of the surrounding colors:
  new_r = sum_red / n
  new_g = sum_green / n
  new_b = sum_blue / n
  # Let's say we can add a random value between -256 and 256 when the
  # separation is half the maximum of $w and $h, and we can only add 0
  # if they're adjacent:
  greatest_difference = Integer( ( 256.0 * separation ) / ([ $w, $h ].max / 2) )

  new_r += $rng.nextInt( 2 * greatest_difference ) - greatest_difference
  new_r = 255 if new_r > 255
  new_r = 0 if new_r < 0

  new_g += $rng.nextInt( 2 * greatest_difference ) - greatest_difference
  new_g = 255 if new_g > 255
  new_g = 0 if new_g < 0

  new_b += $rng.nextInt( 2 * greatest_difference ) - greatest_difference
  new_b = 255 if new_b > 255
  new_b = 0 if new_b < 0

  new_b + (new_g << 8) + (new_r << 16)
end

def to_index( x, y )
  x + y * $w
end

pixels[ 0 ] = random_color
pixels[ to_index( 0, $h - 1 ) ] = random_color
pixels[ to_index( $w - 1, 0 ) ] = random_color
pixels[ to_index( $w - 1, $h - 1 ) ] = random_color

$i.show

fill_in_rectangle( pixels, 0, $w - 1, 0, $h - 1 )

$i.updateAndDraw
