package viewer;

import java.applet.Applet;
import java.awt.*;
import java.awt.geom.*;

public class LinesAntiAliasing extends Applet
{
    private final int SIZE = 300;
    private final int NUMBER_OF_LINES = 30;

    public void paint (Graphics g)
    {
        // recover Graphics2D
        Graphics2D g2 = (Graphics2D)g;

        // draw the left half of the screen using anti-aliased lines
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        for (int i = 0; i < NUMBER_OF_LINES; i++)
        {
            int n = NUMBER_OF_LINES;
            int length = (int)Math.sqrt(SIZE*SIZE + SIZE*SIZE);
            g.drawLine(SIZE, SIZE,
                       SIZE - (int)(length*Math.cos(i*Math.PI/(2*n))),
                       SIZE - (int)(length*Math.sin(i*Math.PI/(2*n))));
        }
        // draw the right half of the screen using default lines
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
        for (int i = 0; i < NUMBER_OF_LINES; i++)
        {
            int n = NUMBER_OF_LINES;
            int length = (int)Math.sqrt(SIZE*SIZE + SIZE*SIZE);
            g.drawLine(SIZE, SIZE,
                       SIZE + (int)(length*Math.cos(i*Math.PI/(2*n))),
                       SIZE - (int)(length*Math.sin(i*Math.PI/(2*n))));
        }
    }//paint
}//LinesAntiAliasing