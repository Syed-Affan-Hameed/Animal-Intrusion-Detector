package com.syed.solarpanelchecker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.icu.util.TimeUnit
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    lateinit var imageView: ImageView;
    lateinit var uploadBtn : Button;
    lateinit var captureBtn : Button;
    lateinit var galleryBtn :Button;
    lateinit var imageUri: Uri;
    lateinit var responseTV: TextView;
    val client = OkHttpClient.Builder()
        .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(600, java.util.concurrent.TimeUnit.SECONDS)
        .build()
//    private val contract=registerForActivityResult(ActivityResultContracts.TakePicture()){
//        imageView.setImageURI(null)
//        imageView.setImageURI(imageUri)
//    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    imageView=findViewById(R.id.imageView);
        captureBtn=findViewById(R.id.captureBtn);
        uploadBtn=findViewById(R.id.uploadBtn);
        galleryBtn=findViewById(R.id.galleryBtn);
        imageUri=createImageUri()!!;
        responseTV=findViewById(R.id.responseTV)

//            imageView.setImageURI(null);
//            contract.launch(imageUri);
            captureBtn.setOnClickListener {
                var intent= Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 111);
            }


        uploadBtn.setOnClickListener {
            responseTV.text="Checking...(ETA 5mins)";
            upload()
        }
        galleryBtn.setOnClickListener {
            val intent= Intent(Intent.ACTION_PICK);
            intent.type="image/*"
            startActivityForResult(intent, 222)
        }
    }



    private fun createImageUri():Uri?{
        val image= File(applicationContext.filesDir, "camera_photo.jpg");
        return FileProvider.getUriForFile(
            applicationContext,
            "com.syed.solarpanelchecker.fileProvider",
            image
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==111){
            var bmp:Bitmap=data?.extras?.get("data") as Bitmap;

            if(bmp==null){
                Toast.makeText(this@MainActivity,"Please capture the image",Toast.LENGTH_LONG).show();
                return
            }
//            imageView.setImageBitmap(bmp);
            imageView.setImageURI(getImageUri(applicationContext, bmp))
            responseTV.text="Click Upload to check Solar panel";

        }
        else if(requestCode==222){

            imageUri= data?.data!!;
            if(imageUri==null){
                Toast.makeText(this@MainActivity,"Please Select the image",Toast.LENGTH_LONG).show();
                return
            }
            imageView.setImageURI(null);
            imageView.setImageURI(data?.data);
            responseTV.text=" Click Upload to check Solar panel ";

        }
        else{
            Toast.makeText(this@MainActivity,"Error",Toast.LENGTH_LONG).show();
        }

    }

    fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path =
            MediaStore.Images.Media.insertImage(
                inContext.getContentResolver(),
                inImage,
                "Title",
                null
            )
        return Uri.parse(path)
    }
    private fun upload(){

        if(imageUri==null){

            Toast.makeText(this@MainActivity, "Error!", Toast.LENGTH_LONG).show();
            return;
        }

        val filesDir=applicationContext.filesDir;
        val file =File(filesDir, "image.jpg")
        val inputStream=contentResolver.openInputStream(imageUri);
        val outputStream=FileOutputStream(file);
        val coroutineExceptionHandler = CoroutineExceptionHandler{ _, throwable ->
            throwable.printStackTrace()
        }
        inputStream!!.copyTo(outputStream);


        val requestBody =file.asRequestBody("image/*".toMediaTypeOrNull())

        val part =MultipartBody.Part.createFormData("File", file.name, requestBody);

        val retrofit=Retrofit.Builder().baseUrl("http://classify.southindia.cloudapp.azure.com/").client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create()
        ).build().create(UploadService::class.java);
        CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
           val response= retrofit.uploadImage(part);
            Log.d("SolarPanelAPI_EndPoint", response.toString())

            responseTV.text=response.toString();
        }

    }
}