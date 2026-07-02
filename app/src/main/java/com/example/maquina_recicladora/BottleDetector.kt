package com.example.maquina_recicladora

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class BottleDetector(
    private val context: Context
) {


    private val interpreter: Interpreter

    private val bottleClass: Int



    init {


        val model =
            loadModelFile()


        interpreter =
            Interpreter(model)



        bottleClass =
            loadBottleClass()



        println(
            "Clase botella encontrada: $bottleClass"
        )

    }



    private fun loadModelFile(): MappedByteBuffer {


        val fileDescriptor =
            context.assets
                .openFd("detect.tflite")


        val inputStream =
            FileInputStream(
                fileDescriptor.fileDescriptor
            )


        val fileChannel =
            inputStream.channel



        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )

    }



    private fun loadBottleClass(): Int {


        val labels =
            context.assets
                .open("labelmap.txt")
                .bufferedReader()
                .readLines()



        val index =
            labels.indexOfFirst {

                it.trim()
                    .equals(
                        "bottle",
                        ignoreCase = true
                    )

            }



        return if(index >= 0){

            index

        }else{

            -1

        }

    }




    fun detect(
        bitmap: Bitmap
    ): Boolean {



        val resized =
            Bitmap.createScaledBitmap(
                bitmap,
                300,
                300,
                true
            )



        val input =
            Array(1){

                Array(300){

                    Array(300){

                        ByteArray(3)

                    }

                }

            }



        for(y in 0 until 300){

            for(x in 0 until 300){


                val pixel =
                    resized.getPixel(
                        x,
                        y
                    )


                input[0][y][x][0] =
                    ((pixel shr 16)
                            and 0xFF)
                        .toByte()


                input[0][y][x][1] =
                    ((pixel shr 8)
                            and 0xFF)
                        .toByte()


                input[0][y][x][2] =
                    (pixel and 0xFF)
                        .toByte()

            }

        }



        val boxes =
            Array(1){

                Array(10){

                    FloatArray(4)

                }

            }



        val classes =
            Array(1){

                FloatArray(10)

            }



        val scores =
            Array(1){

                FloatArray(10)

            }



        val count =
            FloatArray(1)


        val outputs =
            mapOf(
                0 to boxes,
                1 to classes,
                2 to scores,
                3 to count

            )

        interpreter.runForMultipleInputsOutputs(
            arrayOf(input),
            outputs
        )
        for (i in 0 until count[0].toInt()) {

            val claseReal = classes[0][i]
            val clase = claseReal.toInt()
            val confianza = scores[0][i]

            println("----------- Detección $i -----------")
            println("Clase real: $claseReal")
            println("Clase int : $clase")
            println("Confianza : $confianza")
            println("------------------------------------")

            if (confianza > 0.30f) {
                println(">>> DETECCIÓN CONFIABLE <<<")
            }

            if (clase == bottleClass && confianza > 0.60f) {
                println("BOTELLA DETECTADA")
                return true
            }
        }

        return false
    }
}