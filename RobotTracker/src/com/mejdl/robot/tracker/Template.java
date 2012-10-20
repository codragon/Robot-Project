package com.mejdl.robot.tracker;

import org.opencv.core.Rect;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class Template {

	public Rect rect, nomoveRect;
	public Mat templateimage;
	public double area; 

	public Template (){}
	public Template (int x, int y, int w , int h , Mat image)
	{
		if(x >= 0 && y >= 0 && x + w < GlobalVar.ScreenWidth && y + h < GlobalVar.ScreenHeight)
		{
			rect = new Rect(x, y ,w , h);
			templateimage = new Mat(image, rect);
			area = w * h;
			Imgproc.cvtColor(templateimage, templateimage, Imgproc.COLOR_RGBA2GRAY, 1);	// to gray scale
		}
		setNomoveRect();
	}
	
	public Template (Template temp)
	{
		rect = new Rect(temp.rect.x, temp.rect.y,temp.rect.width, temp.rect.height);
		templateimage = new Mat();
		temp.templateimage.copyTo(templateimage);
		area = temp.area;
		
		setNomoveRect();
	}
	
	/*
	 * copy constructor (taking the same rect of temp and template content from image)
	 */
	public Template(Template temp , Mat image)
	{
		rect = new Rect(temp.rect.x, temp.rect.y,image.width(), image.height());
		templateimage = new Mat();
		image.copyTo(templateimage);
		setNomoveRect();
		
	}
	
	private void setNomoveRect()
	{
		nomoveRect = new Rect( (GlobalVar.ScreenWidth/2) - ( (int)Math.round(GlobalVar.noMoveRectRation / 2.0)) * this.rect.width, (GlobalVar.ScreenHeight/2)- ( (int)Math.round(GlobalVar.noMoveRectRation / 2.0)) * this.rect.height , (int)Math.round(GlobalVar.noMoveRectRation * this.rect.width) , (int)Math.round(GlobalVar.noMoveRectRation * this.rect.height));
		
	}

}
