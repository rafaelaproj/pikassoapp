package rafaelacs.com.br.pikassoapp.view;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.media.Image;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class PikassoView extends View {

    public static final float TOUCH_TOLERANCE = 10;
    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private Paint paintScreen;
    private Paint paintLine;
    private HashMap<Integer, Path> pathMap;
    private HashMap<Integer, Point> previousPointMap;


    public PikassoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    void init(){
        paintScreen = new Paint();

        paintLine = new Paint();
        paintLine.setAntiAlias(true);
        paintLine.setColor(Color.BLACK);  //here it can changed the color of the pen
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(5);     //here it can changed the pen width
        paintLine.setStrokeCap(Paint.Cap.ROUND);

        pathMap = new HashMap<>();
        previousPointMap = new HashMap<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
        bitmap.eraseColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas){
        canvas.drawBitmap(bitmap, 0, 0, paintScreen);

        for(Integer key : pathMap.keySet()){
            canvas.drawPath(pathMap.get(key), paintLine);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();   //event type
        int actionIndex = event.getActionIndex();   //pointer (finger, mouse)

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_UP){
            touchStarted(event.getX(actionIndex), event.getY(actionIndex), event.getPointerId(actionIndex));


        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP){
            touchEnded(event.getPointerId(actionIndex));
        } else {
            touchMoved(event);
        }
        invalidate();    //redraw the screen

        return true;
    }

    private void touchMoved(MotionEvent event) {
        for (int i = 0; i < event.getPointerCount(); i++){
            int pointerId = event.getPointerId(i);
            int pointerIndex = event.findPointerIndex(pointerId);

            if(pathMap.containsKey(pointerId)){
                float newX = event.getX(pointerIndex);
                float newY = event.getY(pointerIndex);

                Path path = pathMap.get(pointerId);
                Point point = previousPointMap.get(pointerId);

                //Calculate how far the user moved from the last update
                float deltaX = Math.abs(newX - point.x);
                float deltaY = Math.abs(newY - point.y);

                //if the distance is significant enough to be considered a movement, then...
                if(deltaX >= TOUCH_TOLERANCE || deltaY >= TOUCH_TOLERANCE){
                    //move the path to the new location
                    path.quadTo(point.x, point.y, (newX + point.x) / 2, (newY + point.y) / 2);

                    //store the new coordinates
                    point.x = (int) newX;
                    point.y = (int) newY;
                }
            }
        }
    }

    private void touchEnded(int pointerId) {
        Path path = pathMap.get(pointerId);      //get the corresponding path
        bitmapCanvas.drawPath(path, paintLine);  //draw to BitmapCanvas
        path.reset();
    }

    public void setDrawingColor(int color){
        paintLine.setColor(color);
    }

    public int getDrawingColor(){
        return paintLine.getColor();
    }

    public void setLineWidth(int width){
        paintLine.setStrokeWidth(width);
    }

    public int getLinewidth(){
        return (int) paintLine.getStrokeWidth();
    }

    public void clear(){
        pathMap.clear();    //remove all of the paths
        previousPointMap.clear();
        bitmap.eraseColor(Color.WHITE);
        invalidate();       //refresh the screen
    }

    private void touchStarted(float x, float y, int pointerId) {
        Path path;       //store the path for given touch
        Point point;     //store the last point in path

        if(pathMap.containsKey(pointerId)){
            path = pathMap.get(pointerId);
            point = previousPointMap.get(pointerId);
        } else {
            path = new Path();
            pathMap.put(pointerId, path);
            point = new Point();
            previousPointMap.put(pointerId, point);

        }

        //move the coordinates of the touch
        path.moveTo(x, y);
        point.x = (int) x;
        point.y = (int) y;
    }

    /*public void saveImage(){
        String filename = "Pikasso_" + System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, filename);
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");

        //get a URI for the location save the file
        Uri uri = getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            OutputStream outputStream = getContext().getContentResolver().openOutputStream(uri);

            //copy the bitmap to the outpustream
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);  //our image

            try{
                outputStream.flush();
                outputStream.close();

                Toast message = Toast.makeText(getContext(), "Image Saved!", Toast.LENGTH_LONG);
                message.setGravity(Gravity.CENTER, message.getXOffset()/2, message.getYOffset()/2);
                message.show();
            } catch (IOException e){
                Toast message = Toast.makeText(getContext(), "Image Not Saved!", Toast.LENGTH_LONG);
                message.setGravity(Gravity.CENTER, message.getXOffset()/2, message.getYOffset()/2);
                message.show();
                //e.printStackTrace();
            }
        } catch (FileNotFoundException e){
            Toast message = Toast.makeText(getContext(), "Image Not Saved!", Toast.LENGTH_LONG);
            message.setGravity(Gravity.CENTER, message.getXOffset()/2, message.getYOffset()/2);
            message.show();
            //e.printStackTrace();
        }
    }*/

    public String saveToInternalStorage(){
        ContextWrapper contextWrapper = new ContextWrapper(getContext());
        String filename = "Pikasso_" + System.currentTimeMillis();
        //path to /data/data/app/app_data/imageDir
        File directory = contextWrapper.getDir("imageDir", Context.MODE_PRIVATE);
        //Create imageDir
        File myPath = new File(directory, filename + ".png");

        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(myPath);
            //Use the compress method on the Bitmap object to write image to the outputs
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);   //mesmo com o suposto erro, app roda
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try{
                fos.flush();
                fos.close();
                Log.d("Image:", directory.getAbsolutePath());
                Toast message = Toast.makeText(getContext(), "Image Saved " + directory.getAbsolutePath(), Toast.LENGTH_LONG);
                message.setGravity(Gravity.CENTER, message.getXOffset()/2, message.getYOffset()/2);
                message.show();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        return directory.getAbsolutePath();
    }

    private void loadImageFromStorage(String path){
        try{
            File file = new File(path, "profile.png");
            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
            //ImageView imageView = (ImageView) findViewById(R.id.imagePicker);
            //imageView.setImageBitmap(bitmap);
        } catch (FileNotFoundException e){
            e.printStackTrace();
        }
    }
}
