package com.apple.shubham.captiongenerator

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.bumptech.glide.Glide
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.option_layout.view.*
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.net.URI
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.jar.Manifest

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {


    var filepath: Uri? = null
    lateinit var file: File
    var check = 0
    var textToSpeech:TextToSpeech?=null
    var selectedOption = "VGG"
    lateinit var progressDialog: ProgressDialog
    lateinit var optionArray: Array<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        optionArray = arrayOf("VGG", "VGGFT", "RESNET", "RESNETFT")
        var adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, optionArray)
        option_spinner.adapter = adapter
        option_spinner.onItemSelectedListener = this
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA),
            123
        )

        setImage.setOnClickListener {


            var alertDialog = AlertDialog.Builder(this).create()
            var lf = applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            var view = lf.inflate(R.layout.option_layout, null)
            view.btnCamera.setOnClickListener {
                var cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, 456)
                alertDialog.dismiss()
            }
            view.btnGallery.setOnClickListener {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(intent, 100)
                alertDialog.dismiss()
            }
            alertDialog.setView(view)
            alertDialog.setTitle("Choose Among")
            alertDialog.show()


        }

        btnCaption.setOnClickListener {

            //file=File(filepath!!.path)
//                if(filepath!=null)
//                {
            if (check != 1) {
                var file1 = File(getPath(filepath!!))
                Log.d("aaya", "haan!!!")
                file = file1
            }
            if (selectedOption == optionArray[2]) {
                Log.d("result", selectedOption)

                Log.d("result", file.toString())
                SendToServer().execute("http://192.168.43.167:5001/")
            } else if (selectedOption == optionArray[3]) {
//                    var file1 = File(getPath(filepath!!))
//                    file = file1
                Log.d("result", file.toString())
                SendToServer().execute("http://192.168.43.167:5001/model2")
            } else if (selectedOption == optionArray[0]) {
//                    var file1 = File(getPath(filepath!!))
//                    file = file1
                Log.d("result", file.toString())
                SendToServer().execute("http://192.168.43.167:5001/model3")
            } else {
//                    var file1 = File(getPath(filepath!!))
//                    file = file1
                Log.d("result", file.toString())
                SendToServer().execute("http://192.168.43.167:5001/model4")
            }
//                }


        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedOption = optionArray[position]
    }

    private fun getPath(uri: Uri): String? {
        var projection = arrayOf(MediaStore.Images.Media.DATA)
        var cursor = contentResolver.query(uri, projection, null, null, null)

        if (cursor == null) {
            return null
        }
        var column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        var s = cursor.getString(column_index)
        cursor.close()
        return s
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100) {
            if (data != null) {
                filepath = data.data
                Log.d("result", filepath!!.toString())
                check = 0
            }
            Glide.with(this).load(filepath).into(setImage)
        } else if (requestCode == 456) {
            var photo = data!!.extras.get("data") as Bitmap
            var filesDir = applicationContext.filesDir
            var imageFile = File(filesDir, System.currentTimeMillis().toString() + ".jpg")
            try {
                var os = FileOutputStream(imageFile)
                photo.compress(Bitmap.CompressFormat.JPEG, 100, os)
                file = imageFile
                filepath = Uri.fromFile(file)
                check = 1
                os.flush()
                os.close()
                Glide.with(this).load(file).into(setImage)
            } catch (e: Exception) {
                Log.e("Error writing bitmap", e.toString())
            }
        }
    }

    inner class SendToServer : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {

            val url = URL(params[0])
            val client = OkHttpClient().newBuilder()
                .readTimeout(15, TimeUnit.MINUTES)
                .connectTimeout(15, TimeUnit.MINUTES)
                .writeTimeout(15, TimeUnit.MINUTES)
                .build()

//            Log.d("result",file.toString())


            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", filepath!!.toString(),
                    RequestBody.create(MediaType.parse("image/*"), file)
                )
                .build()
            Log.d("body", body.toString())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()

            return response.body()?.string()

        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            tvResult.text = result
            textToSpeech=TextToSpeech(applicationContext,object:TextToSpeech.OnInitListener{
                override fun onInit(status: Int) {
                    if(status!=TextToSpeech.ERROR)
                    {
                        textToSpeech!!.language = Locale.US
                    }
                }
            } )
            tvResult.setOnClickListener {
                speakOut(result)
            }
            Handler().postDelayed(object :Runnable{
                override fun run() {
                    tvResult.performClick()
                }
            },1000)
            progressDialog.dismiss()
        }

        private fun speakOut(result: String?) {
            textToSpeech!!.speak(result!!,TextToSpeech.QUEUE_FLUSH,null)
        }

        override fun onPreExecute() {
            super.onPreExecute()
            progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            progressDialog.setMessage("Generating caption....")
            progressDialog.setCancelable(false)
            progressDialog.show()
        }
    }

    override fun onStop() {
        super.onStop()
        if(textToSpeech!=null)
        {
            textToSpeech!!.stop()
            textToSpeech!!.shutdown()
        }
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        textToSpeech.stop()
//        textToSpeech.shutdown()
//    }
}
