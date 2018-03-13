package fiji.plugin.trackmate.action;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.io.File;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.LoadTrackMatePlugIn_;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.process.ColorProcessor;

public class CaptureOverlayAction extends AbstractTMAction
{

	public static final ImageIcon ICON = new ImageIcon( TrackSchemeFrame.class.getResource( "resources/camera_go.png" ) );

	public static final String NAME = "Capture overlay";

	public static final String KEY = "CAPTURE_OVERLAY";

	public static final String INFO_TEXT = "<html>" +
			"If the current displayer is the HyperstackDisplayer, this action <br>" +
			"will capture the TrackMate overlay with current display settings. <br>" +
			"That is: a new RGB stack will be created (careful with large data) where <br>" +
			"each frame contains a RGB snapshot of the TrackMate display. " +
			"<p>" +
			"It can take long since we pause between each frame to ensure the whole <br>" +
			"overlay is redrawn. The current zoom is taken into account. <br>" +
			"Also, make sure nothing is moved over the image while capturing. " +
			"</html>";

	@Override
	public void execute( final TrackMate trackmate )
	{
		logger.log( "Capturing TrackMate overlay.\n" );
		logger.log( "  Preparing and allocating memory..." );
		try
		{
			final ImagePlus imp = trackmate.getSettings().imp;
			final ImageWindow win = imp.getWindow();
			win.toFront();
			final Point loc = win.getLocation();
			final ImageCanvas ic = win.getCanvas();
			final Rectangle bounds = ic.getBounds();
			loc.x += bounds.x;
			loc.y += bounds.y;
			final Rectangle r = new Rectangle( loc.x, loc.y, bounds.width, bounds.height );
			final ImageStack stack = new ImageStack( bounds.width, bounds.height );
			Robot robot;
			try
			{
				robot = new Robot();
			}
			catch ( final AWTException e )
			{
				logger.error( "Problem creating the image grabber:\n" + e.getLocalizedMessage() );
				return;
			}
			logger.log( " done.\n" );

			final int nCaptures = imp.getNFrames();

			logger.log( "  Performing capture..." );
			final int channel = imp.getChannel();
			final int slice = imp.getSlice();
			for ( int i = 0; i < nCaptures; i++ )
			{
				logger.setProgress( ( float ) i / nCaptures );
				imp.setPosition( channel, slice, i + 1 );

				IJ.wait( 200 );
				final Image image = robot.createScreenCapture( r );
				final ColorProcessor cp = new ColorProcessor( image );
				stack.addSlice( null, cp );
			}
			new ImagePlus( "TrackMate capture", stack ).show();
			logger.log( " done.\n" );
		}
		finally
		{
			logger.setProgress( 0 );
		}
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new CaptureOverlayAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		final File file = new File( "/Users/tinevez/Google Drive/Projects/Contacts/raw data/2015-09-17/Trackmate files/SiC + SAg2_1_20_BCells.xml" );
		final LoadTrackMatePlugIn_ loader = new LoadTrackMatePlugIn_();
		loader.run( file.getAbsolutePath() );

		loader.getSettings().imp.setDisplayMode( IJ.GRAYSCALE );

		for ( final TrackMateModelView view : loader.getController().getGuimodel().getViews() )
		{
			view.setDisplaySettings( TrackMateModelView.KEY_TRACK_DISPLAY_DEPTH, 100 );
			view.setDisplaySettings( TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD );
		}

		final TrackMate trackmate = loader.getController().getPlugin();
		new CaptureOverlayAction().execute( trackmate );
	}
}
