package com.ubit.lms;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.List;

public class PdfHelper {

    public static void generateAndSavePdf(Context context, String title, List<String> lines) {
        PdfDocument document = new PdfDocument();
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(16);
        titlePaint.setFakeBoldText(true);

        Paint bodyPaint = new Paint();
        bodyPaint.setTextSize(12);

        int pageWidth = 595, pageHeight = 842; // A4 size in points
        int y = 40;
        int pageNumber = 1;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        canvas.drawText(title, 40, y, titlePaint);
        y += 30;

        for (String line : lines) {
            if (y > pageHeight - 40) {
                document.finishPage(page);
                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 40;
            }
            canvas.drawText(line, 40, y, bodyPaint);
            y += 20;
        }

        document.finishPage(page);

        String fileName = title.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream out = context.getContentResolver().openOutputStream(uri);
                    document.writeTo(out);
                    out.close();

                    values.clear();
                    values.put(MediaStore.Downloads.IS_PENDING, 0);
                    context.getContentResolver().update(uri, values, null, null);

                    Toast.makeText(context, "Saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
                }
            } else {
                java.io.File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS);
                java.io.File file = new java.io.File(downloadsDir, fileName);
                OutputStream out = new java.io.FileOutputStream(file);
                document.writeTo(out);
                out.close();
                Toast.makeText(context, "Saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "PDF generation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        document.close();
    }
}