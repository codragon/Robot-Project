package com.mejdl.robot.tracker;


import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.stevehaar.robot.Robot;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

class AppView extends ViewBase {
	private static final String TAG = "View";

	/*
	 * template localization variables
	 */
	private Template Cmin , template , OldLocation, candidate0 = null,candidate1= null,candidate2 = null,candidate3 = null,candidate4=null ; 
	/*
	 * Scaling variables
	 */
	private Template CminS1, CminS2, templateS1, templateS2;
	
	private Robot robot;
   
	/*
	 * color space variables
	 */
	private Mat mYuv;
    private Mat mRgba;
	private Bitmap mBitmap;
	/*
	 * general variables
	 */
	private double r , min = Double.MAX_VALUE;
	private int i , index;

	/*
	 * two modes for the application (Select an object , or start tracking the selected object)
	 */
    public static final int     VIEW_MODE_RGBA_TEMPLATE  = 0;
    public static final int     VIEW_MODE_RGBA_TRACKING  = 1;
	private int mViewMode;
	
    public AppView(Context context) {

        super(context);
        robot = new Robot(context);
        mViewMode = VIEW_MODE_RGBA_TEMPLATE;
    }

	@Override
	protected void onPreviewStarted(int previewWidth, int previewHeight) {
	    synchronized (this) {
        	// initialize Mats before usage
        	mYuv = new Mat(getFrameHeight() + getFrameHeight() / 2, getFrameWidth(), CvType.CV_8UC1);
        	mRgba = new Mat();
        	mBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888); 
    	    }
	}

	@Override
	protected void onPreviewStopped() {
		if(mBitmap != null) {
			mBitmap.recycle();
		}

		synchronized (this) {
            // Explicitly deallocate Mats
            if (mYuv != null)
                mYuv.release();
            if (mRgba != null)
                mRgba.release();
            
            mYuv = null;
            mRgba = null;
        }
    }
	
	public void setViewMode(int viewMode) {
		Log.i(TAG, "setViewMode("+viewMode+")");
		mViewMode = viewMode;
	}

    @Override
    protected Bitmap processFrame(byte[] data , int frameNum) {
    	final int view_mode = mViewMode;
    	int stepsize; double NCCration;
        
    	mYuv.put(0, 0, data);
        Bitmap bmp = mBitmap;
        
        // get the RGBA
        Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV420sp2RGB, 4);
        
        
        // show the frame number on the screen
    	Core.putText(mRgba, "FrameN: " + Integer.toString(frameNum) , new Point(5, 50), 3, 1, new Scalar(255, 0, 0, 255), 2);
    	
        if(view_mode == VIEW_MODE_RGBA_TEMPLATE)			// To select a template 
        {
        	template = new Template(GlobalVar.ScreenWidth/2,GlobalVar.ScreenHeight/2, GlobalVar.TemplateWidth, GlobalVar.TemplateHeight, mRgba);
        	OldLocation = new Template(template);
	        drawRect(template.rect);
	    }
        else	// Start Tracking		
        {
        	stepsize = 3;
        	
        	while(stepsize > 0)
        	{

            	//  at the same location where the template was in the last frame.
            	candidate0 = new Template(OldLocation.rect.x, OldLocation.rect.y, OldLocation.rect.width, OldLocation.rect.height, mRgba);
            	// located right to the template by stepsize pixels. 
            	if(OldLocation.rect.x + stepsize < GlobalVar.ScreenWidth - OldLocation.rect.width)
            		candidate1 = new Template(OldLocation.rect.x + stepsize, OldLocation.rect.y, OldLocation.rect.width, OldLocation.rect.height, mRgba);
            	// located left to the template by stepsize pixels.
            	if(OldLocation.rect.x - stepsize >= 0)
            		candidate2 = new Template(OldLocation.rect.x - stepsize, OldLocation.rect.y, OldLocation.rect.width, OldLocation.rect.height, mRgba);
            	//located down to the template by stepsize pixels.
            	if(OldLocation.rect.y + stepsize < GlobalVar.ScreenHeight - OldLocation.rect.height)
            		candidate3 = new Template(OldLocation.rect.x , OldLocation.rect.y + stepsize , OldLocation.rect.width, OldLocation.rect.height, mRgba);
            	//located up to the template by stepsize pixels.
            	if(OldLocation.rect.y - stepsize >= 0)
            		candidate4 = new Template(OldLocation.rect.x , OldLocation.rect.y - stepsize , OldLocation.rect.width, OldLocation.rect.height, mRgba);
            	
            	
            	// Calculate the mean absolute difference values between the original template and the candidate regions
            	Template  [] candidates  = {candidate0,candidate1,candidate2,candidate3,candidate4};
            	for(min = Double.MAX_VALUE, index = 0, r = 0 , i =0; i < candidates.length; i++)
            	{
            		if(candidates[i] != null)
            		{
	            		r = ComputeMAD(candidates[i].templateimage , template.templateimage); 
	            		if(r < min)
	            		{
	            			min = r; index = i;
	            		}
            		}
            	}
            	
            	Cmin = new Template(candidates[index]);		// the one who minimizes the MAD value will be selected as the new location for the object
            	
            	
            	// if the selected candidate region is at the same location as the one in previous frame, then go back and check the closest neighbors 
            	if((Cmin.rect.x == OldLocation.rect.x) && (Cmin.rect.y == OldLocation.rect.y))	
            	{
            		if(stepsize > 0)
            			stepsize--;
            	}
            	else
            	{	// if else, change the object location using the selected candidate region's location. 

            		OldLocation = new Template(Cmin);
            	}
        	}
        	
        	// Calculate Normalized Cross Correlation NCC between Original template and selected candidate region (OldLocation) to be 
        	// used to determine the need of scaling, template adaptation and if object is occluded. 
        	NCCration = NCC(template , Cmin);
        	
        	
        	
    
        	
        	// 0.8 is a threshold for scaling and adaptation 
        	if(NCCration >= 0.8)
        	{
        		// draw a green circle: the rect is on the template 
        		Core.circle(mRgba, new Point(GlobalVar.ScreenWidth - 150, 50)  ,  10, new Scalar(0, 255, 0, 255) , -2);
        		/*
        		 * Object scale handling
        		 */
 
        		// amount of scaling the candidate region
        		int scaleH = (int)Math.round(Cmin.rect.width * 0.05);
        		int scaleV = (int) Math.round(Cmin.rect.height * 0.05);
            	
        		// scale the candidate region by +-5%
        		CminS1 = new Template(Cmin.rect.x - (scaleH/2) , Cmin.rect.y - (scaleV/2) , Cmin.rect.width + scaleH , Cmin.rect.height + scaleV, mRgba); // 5% scaling
        		CminS2 = new Template(Cmin.rect.x + (scaleH/2) , Cmin.rect.y + (scaleV/2) , Cmin.rect.width - scaleH , Cmin.rect.height - scaleV, mRgba); // 5% scaling
        		
        		// resize the original template to match rescaled candidate regions
        		Mat t1 = new Mat(),t2 = new Mat();
        		Imgproc.resize(template.templateimage, t1, CminS1.templateimage.size(),  0 , 0 , Imgproc.INTER_CUBIC);
        		Imgproc.resize(template.templateimage, t2, CminS2.templateimage.size(),  0 , 0 , Imgproc.INTER_AREA);
        		templateS1 = new Template(template, t1);
        		templateS2 = new Template(template , t2);
        		
        		// get the minimum MAD among all scaled and resized templates
        		Template ScaledCandidates [] = {Cmin , CminS1, CminS2};
        		Template ResizedTemplates [] = {template,templateS1, templateS2};
        		for(index = 0 , min = Double.MAX_VALUE, r = 0, i = 0 ; i < 3 ; i++)
        		{
        			r = ComputeMAD(ScaledCandidates[i].templateimage, ResizedTemplates[i].templateimage);
        			if(r < min)
        			{
        				min = r; index = i;
        			}
        		}
        		
        		// Updating the original template, Candidate region and the Object Location to be used in the next frame.  
        		template = new Template(ResizedTemplates[index]);
        		Cmin = new Template(ScaledCandidates[index]);
        		
        		

             	/*
             	 * Template Adaptation
             	 * Updating the original template due to the change in its appearance 
             	 * 90% of the original template and 10% of the selected candidate region
             	 */
        		Core.putText(mRgba, "Template Adaptation" , new Point(5, 90), 3, 1, new Scalar(255, 0, 0, 255), 2);
        		for(int r = 0 ; r < template.templateimage.height() ; r++)
        			for(int c = 0 ; c < template.templateimage.width()  ; c++)
        				template.templateimage.put(r, c, (0.9 * template.templateimage.get(r, c)[0]) + (0.1 * Cmin.templateimage.get(r, c)[0]));
        	}
        	else
        	{
        		// draw a red circle: the rect isn't on the template
        		Core.circle(mRgba, new Point(GlobalVar.ScreenWidth - 150, 50)  ,  10, new Scalar(255, 0, 0, 255) , -2);
        	}

        	
        	/*
        	 *  Calculating the threshold that determines the occlusion  
        	 */
        	
        	// to be done soon
        	
        	
        	
        	Core.putText(mRgba, "Template Size(" + Integer.toString(template.templateimage.cols()) + "," + 
        	Integer.toString(template.templateimage.rows()) + ")" , new Point(5, 130), 3, 1, new Scalar(255, 0, 0, 255), 2);
        	// draw the new location of the object
        	drawRect(Cmin.rect);
        	candidate0 = null; candidate1 = null; candidate2 = null; candidate3 = null; candidate4 = null;
        }
         
        
       // MoveControlRobot(OldLocation, Cmin);
        try{
        	 Utils.matToBitmap(mRgba, bmp);
        }catch(Exception e) {
            Log.e("View:", "Utils.matToBitmap() throws an exception: " + e.getMessage());
            bmp.recycle();
            bmp = null;
        }
        
        return bmp;
    }
    /*
     * control the robot
     */
    public void  MoveControlRobot(Template OldL, Template NewL)
    {
    	// find out which way to move
    	boolean moveTop = NewL.rect.y < NewL.nomoveRect.y;
		boolean moveRight = NewL.rect.x + NewL.rect.width > NewL.nomoveRect.x + NewL.nomoveRect.width;
		boolean moveBottom = NewL.rect.y + NewL.rect.height > NewL.nomoveRect.y + NewL.rect.height;
		boolean moveLeft = NewL.rect.x < NewL.nomoveRect.x;
	
    	if(moveTop)
    	{
    		if (robot.getCameraVertPosition() < robot.getMaxCameraVertPosition())
    		{
    			robot.MoveCameraVert(robot.getCameraVertPosition() + 1);
    		}
    	}
    	if (moveRight)
    	{
    		if (robot.getCameraHorPosition() < robot.getMaxCameraHorPosition())
    		{
    			robot.MoveCameraVert(robot.getCameraHorPosition() + 1);
    		}
    	}
    	if (moveBottom)
    	{
    		if (robot.getCameraVertPosition() > robot.getMinCameraVertPosition())
    		{
    			robot.MoveCameraVert(robot.getCameraVertPosition() - 1);
    		}
    	}
    	if (moveLeft)
    	{
    		if (robot.getCameraHorPosition() < robot.getMinCameraHorPosition())
    		{
    			robot.MoveCameraVert(robot.getCameraHorPosition() - 1);
    		}
    	}
    }
    
    /*
     * draw a rectangle on the current frame.
     */
    private void drawRect(Rect r)
    {
    	Core.rectangle(mRgba, new Point(r.x, r.y) , new Point(r.x + r.width - 1 , r.y + r.height -1), new Scalar(255, 0, 0));
    }
    
    /*
     * compute the mean absolute differences between two mats 
     */
    private double ComputeMAD(Mat t1 , Mat t2)
    {
    	Mat AD = new Mat();
    	Core.absdiff(t1, t2, AD);
    	return Core.mean(AD).val[0];
    }
    /*
     * compute the normalized cross correlation between template and candidate region
     */
    private double NCC(Template T, Template C)
    {
    	// first calculate the mean intensity values of original T and candidate C
    	double NCCration;
    	double TMIV = Core.mean(T.templateimage).val[0];
    	double CMIV = Core.mean(C.templateimage).val[0];
    	
    	// then calculate Cross correlation
    	double TintensitySubmean = 0;
    	double CintensitySubmean = 0;
    	double productOfTintAndCint = 0;
    	
    	Size size = T.templateimage.size();
    	double rows = size.height;
    	double colums = size.width;
    	
    	for(int r =0 ; r < rows; r++)
    		for(int c= 0 ; c < colums; c++)
    		{
    			TintensitySubmean = TintensitySubmean + Math.pow( T.templateimage.get(r, c)[0] - TMIV , 2);
    			CintensitySubmean = CintensitySubmean + Math.pow(C.templateimage.get(r, c)[0] - CMIV, 2);
    			productOfTintAndCint = productOfTintAndCint + ((T.templateimage.get(r, c)[0] - TMIV) * (C.templateimage.get(r, c)[0] - CMIV));
    		}
    	
    	
    	// Now we have all the items for computing NCC
    	
    	NCCration = (productOfTintAndCint / (Math.sqrt(TintensitySubmean/T.templateimage.total()) * Math.sqrt(CintensitySubmean/C.templateimage.total()))) / T.templateimage.total() ;
    	return NCCration;
    }
}
