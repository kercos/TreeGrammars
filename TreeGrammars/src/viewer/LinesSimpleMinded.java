/*
    This applet draws straight lines in a simple minded way
    by just plotting y as a function of x; this doesn't draw
    very good lines for angles more than 45 degrees. This applet
    also draws standard Java lines for comparison.

    This applet assumes that the graphics window is twice
    as long as it is high.
*/
package viewer;
import java.awt.*;
import java.applet.Applet;

public class LinesSimpleMinded extends Applet
{
    private final int SIZE = 300;
    private final int NUMBER_OF_LINES = 30;

    public void paint (Graphics g)
    {
        // draw simple minded lines in the left half of the window
        for (int i = 0; i < NUMBER_OF_LINES; i++)
        {
            // this inner loop plots the points of one line
            for (int x = 0; x <= SIZE; x++)
            {
                int n = NUMBER_OF_LINES;
                double slope = Math.tan(i*Math.PI/(2*n));
                int y = (int)(slope*x);
                // plot one point of the line
                g.drawLine(SIZE-x, SIZE-y, SIZE-x, SIZE-y);
            }
        }
        // draw regular Java lines in the right half of the window
        for (int i = 0; i < NUMBER_OF_LINES; i++)
        {
            int n = NUMBER_OF_LINES;
            int length = (int)Math.sqrt(SIZE*SIZE + SIZE*SIZE);
            g.drawLine(SIZE, SIZE,
                       SIZE + (int)(length*Math.cos(i*Math.PI/(2*n))),
                       SIZE - (int)(length*Math.sin(i*Math.PI/(2*n))));
        }
    }//paint

}//LinesSimpleMinded