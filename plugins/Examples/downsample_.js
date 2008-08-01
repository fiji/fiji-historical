var imp = WindowManager.getCurrentImage();
var width = 0;
var height = 0;
var sourceSigma = 0.5;
var targetSigma = 0.5;
var widthField;
var heightField;
var fieldWithFocus;

var textListener = new java.awt.event.TextListener(
	{
		textValueChanged : function( e )
		{
			var source = e.getSource();
			var newWidth = Math.round( widthField.getText() );
			var newHeight = Math.round( heightField.getText() );
        	
        	if ( source == widthField && fieldWithFocus == widthField && newWidth )
        	{
            	newHeight = Math.round( newWidth * imp.getHeight() / imp.getWidth() );
            	heightField.setText( newHeight );
            }
            else if ( source == heightField && fieldWithFocus == heightField && newHeight )
            {
            	newWidth = Math.round( newHeight * imp.getWidth() / imp.getHeight() );
            	widthField.setText( newWidth );
            }
        } 
	} );

var focusListener = new java.awt.event.FocusListener(
	{
		focusGained : function ( e )
		{
        	fieldWithFocus = e.getSource();
        },
		focusLost : function( e ){} 
	} );

if ( imp )
{
	width = imp.getWidth();
	height = imp.getHeight();
	
	gd = new GenericDialog( "Downsample" );
	gd.addNumericField( "width :", width, 0 );
	gd.addNumericField( "height :", height, 0 );
	gd.addNumericField( "source sigma :", sourceSigma, 2 );
	gd.addNumericField( "target sigma :", targetSigma, 2 );
	var fields = gd.getNumericFields();
	
	widthField = fields.get( 0 );
	heightField = fields.get( 1 );
	fieldWithFocus = widthField;
	
	widthField.addFocusListener( focusListener );
	widthField.addTextListener( textListener );
	heightField.addFocusListener( focusListener );
	heightField.addTextListener( textListener );
		
	gd.showDialog();
	if ( gd.wasOKed() )
	{
		width = gd.getNextNumber();
		height = gd.getNextNumber();
		sourceSigma = gd.getNextNumber();
		targetSigma = gd.getNextNumber();
		
		if ( width <= imp.getWidth() )
		{
			var s;
			if ( fieldWithFocus == widthField )
				s = targetSigma * imp.getWidth() / width;
			else
				s = targetSigma * imp.getHeight() / height;

			IJ.run( "Gaussian Blur...", "sigma=" + Math.sqrt( s * s - sourceSigma * sourceSigma ) );
			IJ.run( "Scale...", "x=- y=- width=" + width + " height=" + height + " title=-" );
			IJ.run( "Canvas Size...", "width=" + width + " height=" + height + " position=Center" );
		}
		else
			IJ.showMessage( "You try to upsample the image.  You need an interpolator for that not a downsampler." );
	}
}
else
	IJ.showMessage( "You should have at least one image open." );
