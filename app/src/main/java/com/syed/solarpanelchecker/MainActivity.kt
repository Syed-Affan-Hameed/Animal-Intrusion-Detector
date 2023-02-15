package com.syed.solarpanelchecker

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    lateinit var imageView: ImageView;
    lateinit var uploadBtn : Button;
    lateinit var captureBtn : Button;
    lateinit var galleryBtn :Button;
    lateinit var imageUri: Uri;
    lateinit var responseTV: TextView;
    private val contract=registerForActivityResult(ActivityResultContracts.TakePicture()){
        imageView.setImageURI(null)
        imageView.setImageURI(imageUri)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView=findViewById(R.id.imageView);
        captureBtn=findViewById(R.id.captureBtn);
        uploadBtn=findViewById(R.id.uploadBtn);
        galleryBtn=findViewById(R.id.galleryBtn);
        imageUri=createImageUri()!!;
        responseTV=findViewById(R.id.responseTV)
        captureBtn.setOnClickListener {
            contract.launch(imageUri);

        }

        uploadBtn.setOnClickListener {
            upload()
        }
        galleryBtn.setOnClickListener {
            val intent= Intent(Intent.ACTION_PICK);
            intent.type="image/*"
            startActivityForResult(intent,111)
        }
    }



    private fun createImageUri():Uri?{
        val image= File(applicationContext.filesDir,"camera_photo.jpg");
        return FileProvider.getUriForFile(applicationContext,"com.syed.solarpanelchecker.fileProvider",image)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==111){
            imageUri=data?.data!!;
            imageView.setImageURI(null);
            imageView.setImageURI(data?.data)
        }
    }
    private fun upload(){

        if(imageUri==null){

            Toast.makeText(this@MainActivity,"Error!", Toast.LENGTH_LONG).show();
            return;
        }

        val filesDir=applicationContext.filesDir;
        val file =File(filesDir,"image.jpg")
        val inputStream=contentResolver.openInputStream(imageUri);
        val outputStream=FileOutputStream(file);
        inputStream!!.copyTo(outputStream);


        val requestBody =file.asRequestBody("image/*".toMediaTypeOrNull())

        val part =MultipartBody.Part.createFormData("profile",file.name,requestBody);

        val retrofit=Retrofit.Builder().baseUrl("http://classify.southindia.cloudapp.azure.com/").addConverterFactory(GsonConverterFactory.create()).build().create(UploadService::class.java);
        CoroutineScope(Dispatchers.IO).launch {
           val response= retrofit.uploadImage(part);
            Log.d("SolarPanelAPI_EndPoint", response.toString())
            responseTV.text=response.toString();
        }

    }
}