package jakobkarolus.de.pulseradar.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.widget.Toast;

/**
 * Created by Jakob on 04.05.2015.
 */
public class SpectrogramView extends View{

    private Paint paint = new Paint();
    private Bitmap bmp;


    public SpectrogramView(Context context, int[][] data) {
        super(context);

        if (data != null) {
            paint.setStrokeWidth(1);
            int height = data[0].length;
            int width = data.length;
            Toast.makeText(getContext(), "#frequencies: " + height + ", #timesteps: " + width, Toast.LENGTH_LONG).show();

            int[] arrayCol = new int[width*height];
            int counter = 0;
            for(int i = height-1; i >= 0; i--) {
                for(int j = 0; j < width; j++) {
                        /*
                        int value;
                        int color;
                        value = 255 - (int)(data[j][i] * 255);
                        color = (value<<16|value<<8|value|255<<24);
                        */
                    arrayCol[counter] = data[j][i];
                    counter ++;
                }
            }
            bmp = Bitmap.createBitmap(arrayCol, width, height, Bitmap.Config.ARGB_8888);

        } else {
            System.err.println("Data Corrupt");
        }
    }

    public Bitmap getBitmap(){
        return bmp;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(bmp, 0, 100, paint);
    }
}
