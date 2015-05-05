package jakobkarolus.de.pulseradar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * Created by Jakob on 04.05.2015.
 */
public class SpectrogramView extends View{

    private Paint paint = new Paint();
    private Bitmap bmp;


    public SpectrogramView(Context context, RGB [][] data) {
        super(context);

        if (data != null) {
            paint.setStrokeWidth(1);
            int width = data.length;
            int height = data[0].length;

            int[] arrayCol = new int[width*height];
            int counter = 0;
            for(int i = 0; i < height; i++) {
                for(int j = 0; j < width; j++) {
                        /*
                        int value;
                        int color;
                        value = 255 - (int)(data[j][i] * 255);
                        color = (value<<16|value<<8|value|255<<24);
                        */
                    arrayCol[counter] = Color.rgb(data[i][j].r, data[i][j].g, data[i][j].b);
                    counter ++;
                }
            }
            bmp = Bitmap.createBitmap(arrayCol, width, height, Bitmap.Config.ARGB_8888);

        } else {
            System.err.println("Data Corrupt");
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(bmp, 0, 100, paint);
    }
}

class RGB{

    protected int r;
    protected int g;
    protected int b;

    public RGB(int r, int g, int b){
        this.r = r;
        this.g = g;
        this.b = b;
    }
}
