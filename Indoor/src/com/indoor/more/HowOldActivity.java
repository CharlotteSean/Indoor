package com.indoor.more;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.facepp.error.FaceppParseException;
import com.gitonway.lee.niftynotification.lib.Effects;
import com.indoor.im.R;
import com.indoor.im.ui.ActivityBase;
import com.indoor.im.view.HeaderLayout.onRightImageButtonClickListener;
import com.indoor.more.util.FaceppDetect;
import com.indoor.more.util.FaceppDetect.CallBack;
import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class HowOldActivity extends ActivityBase {

	protected static final int PICK_CODE = 0x110;
	private static final int MSG_SUCCESS=0x111;
	private static final int MSG_ERROR=0x112;
	private ImageView mphoto;
	private CircleProgressBar progress;
	private String mCurrentPhotoStr;
	private Bitmap mPhotoImg;
	private Paint mPaint; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_how_old);
		initTopBarForBoth("How-old", R.drawable.base_action_bar_add_bg_selector,new onRightImageButtonClickListener() {
			
			@Override
			public void onClick() {
				Intent intent=new Intent(Intent.ACTION_PICK);
				intent.setType("image/*");
				startActivityForResult(intent, PICK_CODE);
			}
		});
		mphoto=(ImageView) findViewById(R.id.id_photo);
		progress=(CircleProgressBar) findViewById(R.id.progress);
		progress.setVisibility(View.GONE);
		mPaint=new Paint();
	}
	
	@Override
	protected void onActivityResult(int arg0, int arg1, Intent arg2) {
		if(arg0==PICK_CODE){
			if(arg2!=null){
				Uri uri=arg2.getData();
				Cursor cursor=getContentResolver().query(uri, null, null, null, null);
				cursor.moveToFirst();
				int idx=cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
				mCurrentPhotoStr=cursor.getString(idx);
				cursor.close();
				resizePhoto();
			}
		}
		super.onActivityResult(arg0, arg1, arg2);
	}
	
	private Handler mhandler=new Handler(){
		
		public void handleMessage(Message msg) {
			
			switch(msg.what){
				case MSG_SUCCESS:
					JSONObject rs=(JSONObject) msg.obj;
					prepareRsBitmap(rs);
					break;
					
				case MSG_ERROR:
					//String errormsg=(String) msg.obj;
					showTag("�������������!",Effects.jelly,R.id.howold);
					break;
			}
		}
	};

	//����ͼƬ
	private void resizePhoto() {
		BitmapFactory.Options options=new BitmapFactory.Options();
		options.inJustDecodeBounds=true;
		BitmapFactory.decodeFile(mCurrentPhotoStr,options);
		double ratio=Math.max(options.outWidth*1.0d/1024f, options.outHeight*1.0d/1024f);
		options.inSampleSize=(int) Math.ceil(ratio);
		options.inJustDecodeBounds=false;
		mPhotoImg = BitmapFactory.decodeFile(mCurrentPhotoStr,options);
		mphoto.setImageBitmap(mPhotoImg);
		progress.setVisibility(View.VISIBLE);
		FaceppDetect.detect(mPhotoImg, new CallBack() {
			
			@Override
			public void success(JSONObject result) {
				Message message=Message.obtain();
				message.what=MSG_SUCCESS;
				message.obj=result;
				mhandler.sendMessage(message);
			}
			
			@Override
			public void error(FaceppParseException exception) {
				Message message=Message.obtain();
				message.what=MSG_ERROR;
				message.obj=exception.getErrorMessage();
				mhandler.sendMessage(message);
			}
		});
	}

	//�������������JSon
	protected void prepareRsBitmap(JSONObject rs) {
		
		Bitmap bitmap=Bitmap.createBitmap(mPhotoImg.getWidth(),mPhotoImg.getHeight(),mPhotoImg.getConfig());
		Canvas canvas=new Canvas(bitmap);
		canvas.drawBitmap(mPhotoImg, 0, 0,null);
		try {
			JSONArray faces=rs.getJSONArray("face");
			int faceCount=faces.length();
			if(faceCount==0)
				showTag("û�м�⵽����",Effects.jelly,R.id.howold);
			for(int i=0;i<faceCount;i++){
				
				JSONObject face=faces.getJSONObject(i);
				JSONObject posObj=face.getJSONObject("position");
				float x=(float) posObj.getJSONObject("center").getDouble("x");
				float y=(float) posObj.getJSONObject("center").getDouble("y");
				float w=(float) posObj.getDouble("width");
				float h=(float) posObj.getDouble("height");
				
				x=x/100*bitmap.getWidth();
				y=y/100*bitmap.getHeight();
				w=w/100*bitmap.getWidth();
				h=h/100*bitmap.getHeight();
				mPaint.setColor(0xffffffff);
				mPaint.setStrokeWidth(3);
				canvas.drawLine(x-w/2, y-h/2, x-w/2, y+h/2,mPaint);
				canvas.drawLine(x-w/2, y-h/2, x+w/2, y-h/2,mPaint);
				canvas.drawLine(x+w/2, y-h/2, x+w/2, y+h/2,mPaint);
				canvas.drawLine(x-w/2, y+h/2, x+w/2, y+h/2,mPaint);
				
				int age=face.getJSONObject("attribute").getJSONObject("age").getInt("value");
				String gender=face.getJSONObject("attribute").getJSONObject("gender").getString("value");
				Bitmap ageBitmap=buildAgeBitmap(age,"Male".equals(gender));
				int ageWidth=ageBitmap.getWidth();
				int ageHeight=ageBitmap.getHeight();
				if(ageHeight<mphoto.getHeight() && ageWidth<mphoto.getWidth()){
					float ratio=Math.max(bitmap.getWidth()*1.0f/mphoto.getWidth(), bitmap.getHeight()*1.0f/mphoto.getHeight());
					ageBitmap=Bitmap.createScaledBitmap(ageBitmap, (int)(ageWidth*ratio), (int)(ageHeight*ratio), false);
				}
				canvas.drawBitmap(ageBitmap, x-ageBitmap.getWidth()/2, y-h/2-ageBitmap.getHeight(),null);
				mPhotoImg=bitmap;
			}
			mphoto.setImageBitmap(mPhotoImg);
			progress.setVisibility(View.GONE);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	//����������Ϣͼ
	private Bitmap buildAgeBitmap(int age, boolean isMale) {
		TextView tv=(TextView) findViewById(R.id.id_age_and_gender);
		tv.setText(age+"");
		if(isMale)
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male), null, null,null);
		else
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female), null, null, null);
		tv.setDrawingCacheEnabled(true);
		Bitmap bitmap=Bitmap.createBitmap(tv.getDrawingCache());
		tv.destroyDrawingCache();
		return bitmap;
	}
}
