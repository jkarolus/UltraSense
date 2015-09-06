package jakobkarolus.de.ultrasense.gallery;

import android.app.Fragment;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import jakobkarolus.de.ultrasense.R;
import jakobkarolus.de.ultrasense.UltraSenseModule;
import jakobkarolus.de.ultrasense.features.gestures.CalibrationState;
import jakobkarolus.de.ultrasense.features.gestures.Gesture;
import jakobkarolus.de.ultrasense.features.gestures.GestureCallback;

/**
 * Fragment for the gallery activity
 * <br><br>
 * Created by Jakob on 06.09.2015.
 */
public class UltraSenseGalleryFragment extends Fragment implements GestureCallback {

    private List<String> imageIds;
    private UltraSenseModule ultraSenseModule;
    private ViewPager mViewPager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.ultraSenseModule = new UltraSenseModule(getActivity());
        imageIds = new Vector<>();

        ContentResolver cr = getActivity().getContentResolver();
        String[] columns = new String[]{MediaStore.Images.ImageColumns.DATA};
        Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, null);
        cursor.moveToFirst();
        while(cursor.moveToNext()){
            imageIds.add(cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_ultrasense_gallery, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        PagerAdapter mAdapter = new CustomPagerAdapter();
        mViewPager.setAdapter(mAdapter);
        //iv = (ImageView) view.findViewById(R.id.gallery_image);

    }

    private void startGestureDetection() {
        ultraSenseModule.createGestureDetector(UltraSenseGalleryFragment.this, false, true);
        ultraSenseModule.startDetection();

    }

    private void stopGestureDetection() {
        ultraSenseModule.stopDetection();
    }

    @Override
    public void onStart() {
        super.onStart();
        startGestureDetection();
    }

    @Override
    public void onStop() {
        stopGestureDetection();
        super.onStop();
    }

    @Override
    public void onCalibrationStep(CalibrationState calibState) {

        //no calibration in this example
    }

    @Override
    public void onCalibrationFinished(Map<String, Double> thresholds, String prettyPrintThresholds, String name) {
        //no calibration in this example

    }

    @Override
    public void onGestureDetected(final Gesture gesture) {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(gesture == Gesture.SWIPE){
                    mViewPager.setCurrentItem((mViewPager.getCurrentItem()+1)%imageIds.size());
                }
                if(gesture == Gesture.DOWN_UP){
                    mViewPager.setCurrentItem((mViewPager.getCurrentItem()-1)%imageIds.size());
                }
            }
        });
    }

    private class CustomPagerAdapter extends PagerAdapter{


        @Override
        public int getCount() {
            return imageIds.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return o == view;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "TODO";
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }


        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.ultrasense_gallery_pager_view, container, false);
            container.addView(view);
            ImageView iv = (ImageView) view.findViewById(R.id.gallery_image);
            iv.setImageBitmap(decodeSampledBitmapFromResource(imageIds.get(position), 500, 500));
            //iv.setImageBitmap(BitmapFactory.decodeFile(imageIds.get(position)));
            //TextView tv = (TextView) view.findViewById(R.id.gallery_image);
            //tv.setText("Test " + imageIds.get(position));

            return view;
        }
    }


    private Bitmap decodeSampledBitmapFromResource(String filePath, int reqWidth, int reqHeight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    private int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}
