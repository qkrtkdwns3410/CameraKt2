package com.example.camerakt

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOError
import java.io.IOException
import java.security.Permission
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    val REQUEST_IMAGE_CAPTURE =1 //카메라, 사진 촬영 요청 코드입니다.
    lateinit var curPhotopath : String // 문자열 형태의 사진 경로 값 (초기 값을 null로 시작하고 싶을 때)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setPermission() //최초의 권한을 체크하는 메소드

        btn_Camera.setOnClickListener{
            takeCapture() //기본 카메라앱을 실행하여 카메라 촬영
        }
    }

    /**
     * 카메라 촬영
     */
    private fun takeCapture() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also {takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager).also {
                val PhotoFile: File? = try {
                    createImageFile()
                } catch (ex:IOException) {
                    null
                }
                PhotoFile?.also {
                    val photoURI : Uri = FileProvider.getUriForFile(
                        this,"com.example.camerakt.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent,REQUEST_IMAGE_CAPTURE)
                }

            }
        }
    }

    /**
     * 이미지 파일 생성
     */
    private fun createImageFile(): File? {
        val timestamp :String = SimpleDateFormat("yyyyMMDD_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timestamp}_","jpg",storageDir)
            .apply { curPhotopath = absolutePath}
    }

    //테드 퍼미션
    private fun setPermission() { //테드 퍼미션 설정 권한허용 팝업을 만드는것
        val permission = object :PermissionListener{
            override fun onPermissionGranted() {        //설정해놓은 위험 권한들이 허용되었을 경우 수행
                Toast.makeText(this@MainActivity,"권한이 허용되었습니다",Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) { //설정해놓은 위험 권한들중 거부를 한 경우 해당 수행
                Toast.makeText(this@MainActivity,"권한이 거부되었습니다",Toast.LENGTH_SHORT).show()
            }

        }

        TedPermission.with(this)
            .setPermissionListener(permission)
            .setRationaleMessage("카메라앱을 사용하시려면 권한을 허용해주세요")
            .setDeniedMessage("권한을 거부하셨습니다 [앱 설정] -> [권한] 항목에서 허용해주세요")
            .setPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,android.Manifest.permission.CAMERA)
            .check()


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { //
        // startActivityForREsult를 통해 기본 카메라앱으로 부터 받아온 사진 결과 값
        super.onActivityResult(requestCode, resultCode, data)

        //이미지를 성공적으로 가져왔다면?
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode ==Activity.RESULT_OK){
            val bitmap : Bitmap
            val file = File(curPhotopath)

            if (Build.VERSION.SDK_INT < 28 ) //안드로이드 파이(9.0)보다 낮을 경우
            {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver,Uri.fromFile(file))
                tv_Profile.setImageBitmap(bitmap)
            }else {     //안드로이드 9.0버전이거나 그거보다 높을 경우
                val decode = ImageDecoder.createSource(
                    this.contentResolver,
                    Uri.fromFile(file)
                )

                bitmap =  ImageDecoder.decodeBitmap(decode)
                tv_Profile.setImageBitmap(bitmap)

            }
            savePhoto(bitmap)
        }



    }
/**
 *
 */
    private fun savePhoto(bitmap: Bitmap) {
        val folderPath = Environment.getExternalStorageDirectory().absolutePath + "/Pictures/"
        //사진폴더로 저장하기 위한 경로 선언
        val timestamp :String = SimpleDateFormat("yyyyMMDD_HHmmss").format(Date())
        val fileName = "${timestamp}.jpeg"
        val folder = File(folderPath)
        if(!folder.isDirectory) //현재 해당경로에 폴더가 존재하지않는다면? (! ->부정구문)
        {
            folder.mkdirs() //make directory의 준말로 해당 경로에 폴더 자동으로 만들기
        }
        val out  = FileOutputStream(folderPath + fileName)
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,out)
        Toast.makeText(this,"사진이 앨범에 저장되었습니다",Toast.LENGTH_SHORT).show()

    }
}











