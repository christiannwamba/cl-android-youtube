package com.example.android.youtubeapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager
import android.util.Log
import android.widget.Toast
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class MainActivity : AppCompatActivity(),RecyclerAdapter.OnItemClickedListener {

    lateinit var videosAdapter:RecyclerAdapter
    private val CHOOSE_VIDEO: Int=100
    private val STORAGE_PERMISSION: Int = 200
    private var alreadyFetching:Boolean = false
    lateinit var dialog:LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fetchResources()
        dialog = LoadingDialog(this@MainActivity)
        swiperefresh.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener {
            if (!alreadyFetching)
                fetchResources()
        })

        fab.setOnClickListener {
            if (checkStoragePermission()){
                selectVideo()
            } else {
                ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        STORAGE_PERMISSION)
            }

        }
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this@MainActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == CHOOSE_VIDEO && resultCode == Activity.RESULT_OK) {
            MediaManager.get()
                    .upload(data!!.data)
                    .unsigned("ddsfdcvz")
                    .option("resource_type", "video")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            dialog.show()
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            Log.d("TAG",bytes.toString()+"/"+totalBytes)
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            dialog.dismiss()
                            Toast.makeText(this@MainActivity, "Upload successful", Toast.LENGTH_LONG).show()
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            dialog.dismiss()
                            Toast.makeText(this@MainActivity,"Upload was not successful",Toast.LENGTH_LONG).show()
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            dialog.dismiss()
                            Toast.makeText(this@MainActivity,"Reschedule",Toast.LENGTH_LONG).show()
                        }
                    }).dispatch()

        }
    }

    override fun onItemClicked(selectedVideo: Resource) {
        val intent = Intent(this@MainActivity,VideoActivity::class.java)
        intent.putExtra("video_url",selectedVideo.url)
        intent.putExtra("video_name",selectedVideo.publicId)
        startActivity(intent)
    }

    private fun fetchResources() {
        swiperefresh.isRefreshing = true
        alreadyFetching = true
        getRetrofitClient().getVideos().enqueue(object : Callback<Model> {
            override fun onResponse(call: Call<Model>?, response: Response<Model>?) {
                val model = response!!.body()!!
                setupRecyclerView()
                swiperefresh.isRefreshing = false
                alreadyFetching = false
                videosAdapter.setItems(model.resources)
            }

            override fun onFailure(call: Call<Model>?, t: Throwable?) {
                swiperefresh.isRefreshing = false
                alreadyFetching = false
            }

        })

    }

    private fun getRetrofitClient(): ApiInterface {
        val httpClient = OkHttpClient.Builder()
        val builder = Retrofit.Builder()
                .baseUrl("http://10.0.3.2:9000/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())

        val retrofit = builder
                .client(httpClient.build())
                .build()
        return retrofit.create(ApiInterface::class.java)
    }

    private fun setupRecyclerView() {
        with(recycler_view){
            layoutManager = StaggeredGridLayoutManager(3,1)
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = RecyclerAdapter(this@MainActivity)
            videosAdapter = adapter as RecyclerAdapter
        }
    }

    private fun selectVideo() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, CHOOSE_VIDEO)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectVideo()
        }
    }

}
