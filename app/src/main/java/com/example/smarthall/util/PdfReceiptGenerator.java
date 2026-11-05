package com.example.smarthall.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.smarthall.model.PaymentRecord;
import com.example.smarthall.model.Student;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfReceiptGenerator {

    public static File generateAndOpenReceipt(Context context, PaymentRecord payment, Student student, BillingEngine.BillResult billResult) {
        if (context == null || payment == null) {
            return null;
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.parseColor("#B71C1C"));
        canvas.drawRect(0, 0, 595, 110, headerPaint);

        Paint whiteTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteTitlePaint.setColor(Color.WHITE);
        whiteTitlePaint.setTextSize(22);
        whiteTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        whiteTitlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Amar Ekushey Hall", 595 / 2f, 42, whiteTitlePaint);

        Paint whiteSubtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteSubtitlePaint.setColor(Color.WHITE);
        whiteSubtitlePaint.setTextSize(12);
        whiteSubtitlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Khulna University of Engineering & Technology", 595 / 2f, 68, whiteSubtitlePaint);

        Paint whiteBadgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteBadgePaint.setColor(Color.WHITE);
        whiteBadgePaint.setTextSize(14);
        whiteBadgePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        whiteBadgePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Official Money Receipt", 595 / 2f, 95, whiteBadgePaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.parseColor("#FFCDD2"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.2f);

        Paint lightBgPaint = new Paint();
        lightBgPaint.setColor(Color.parseColor("#FFF8F8"));
        lightBgPaint.setStyle(Paint.Style.FILL);

        Paint textPrimaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPrimaryPaint.setColor(Color.parseColor("#212121"));
        textPrimaryPaint.setTextSize(11);

        Paint textPrimaryBold = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPrimaryBold.setColor(Color.parseColor("#212121"));
        textPrimaryBold.setTextSize(11);
        textPrimaryBold.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint textRedBold = new Paint(Paint.ANTI_ALIAS_FLAG);
        textRedBold.setColor(Color.parseColor("#B71C1C"));
        textRedBold.setTextSize(12);
        textRedBold.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint textSecondaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textSecondaryPaint.setColor(Color.parseColor("#757575"));
        textSecondaryPaint.setTextSize(10);

        RectF metaRect = new RectF(40, 125, 555, 185);
        canvas.drawRoundRect(metaRect, 8, 8, lightBgPaint);
        canvas.drawRoundRect(metaRect, 8, 8, borderPaint);

        canvas.drawText("Transaction ID: " + payment.getTrxId(), 52, 148, textRedBold);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US);
        long paymentTime = payment.getPaidAt() > 0 ? payment.getPaidAt() : System.currentTimeMillis();
        String dateStr = sdf.format(new Date(paymentTime));
        Paint textRightBold = new Paint(textPrimaryBold);
        textRightBold.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Date: " + dateStr, 543, 148, textRightBold);

        String targetDateStr = billResult != null ? billResult.getTargetDate() : new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date(paymentTime));
        canvas.drawText("Billing Period: From 01/10/2026 to " + targetDateStr, 52, 170, textSecondaryPaint);

        RectF studentRect = new RectF(40, 198, 555, 298);
        canvas.drawRoundRect(studentRect, 8, 8, lightBgPaint);
        canvas.drawRoundRect(studentRect, 8, 8, borderPaint);

        canvas.drawText("Student Particulars", 52, 218, textRedBold);

        String sName = student != null && student.getName() != null ? student.getName() : "Student";
        String sRoll = student != null && student.getRollNumber() != null ? student.getRollNumber() : payment.getRoll();
        String sDept = student != null && student.getDepartment() != null ? student.getDepartment() : "N/A";
        String sRoom = student != null && student.getAllocatedRoom() != null ? student.getAllocatedRoom() : "N/A";
        String sBoarder = student != null && student.getBoarderNo() != null ? student.getBoarderNo() : "N/A";
        String sDining = student != null && student.getDining() != null ? student.getDining() : "East";
        String cleanDining = sDining.replaceAll("(?i)\\bhall\\b", "").trim();
        if (cleanDining.isEmpty()) cleanDining = "East";

        canvas.drawText("Name: " + sName, 52, 242, textPrimaryPaint);
        canvas.drawText("Department: " + sDept, 52, 262, textPrimaryPaint);
        canvas.drawText("Boarder No: " + sBoarder, 52, 282, textPrimaryPaint);

        canvas.drawText("Roll Number: " + sRoll, 320, 242, textPrimaryPaint);
        canvas.drawText("Room No: " + sRoom, 320, 262, textPrimaryPaint);
        canvas.drawText("Dining: " + cleanDining, 320, 282, textPrimaryPaint);

        RectF tableHeaderRect = new RectF(40, 312, 555, 340);
        canvas.drawRect(tableHeaderRect, headerPaint);

        Paint tableHeaderWhite = new Paint(Paint.ANTI_ALIAS_FLAG);
        tableHeaderWhite.setColor(Color.WHITE);
        tableHeaderWhite.setTextSize(11);
        tableHeaderWhite.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint tableHeaderWhiteRight = new Paint(tableHeaderWhite);
        tableHeaderWhiteRight.setTextAlign(Paint.Align.RIGHT);

        canvas.drawText("Description", 52, 330, tableHeaderWhite);
        canvas.drawText("Quantity / Period", 280, 330, tableHeaderWhite);
        canvas.drawText("Amount (BDT)", 543, 330, tableHeaderWhiteRight);

        double paidNow = payment.getAmount();
        double mealBill = billResult != null ? billResult.getMealBill() : (paidNow * 0.67);
        int mealDays = billResult != null ? billResult.getActiveMealDays() : (int) Math.round(mealBill / 65.0);
        double hallFee = billResult != null ? billResult.getHallFee() : (paidNow - mealBill);
        int hallMonths = billResult != null ? billResult.getHallMonths() : Math.max(1, (int) Math.round(hallFee / 250.0));
        double grossBill = billResult != null ? billResult.getGrossBill() : paidNow;
        double alreadyPaid = billResult != null ? billResult.getAlreadyPaid() : 0.0;

        Paint dividerPaint = new Paint();
        dividerPaint.setColor(Color.parseColor("#EEEEEE"));
        dividerPaint.setStrokeWidth(1);

        Paint rightTextPrimary = new Paint(textPrimaryPaint);
        rightTextPrimary.setTextAlign(Paint.Align.RIGHT);

        canvas.drawText("Active Meal Days (x 65 BDT)", 52, 365, textPrimaryPaint);
        canvas.drawText(mealDays + " Days", 280, 365, textPrimaryPaint);
        canvas.drawText(String.format(Locale.US, "%.0f BDT", mealBill), 543, 365, rightTextPrimary);
        canvas.drawLine(40, 378, 555, 378, dividerPaint);

        canvas.drawText("Hall Extra Utility Fee (x 250 BDT/month)", 52, 400, textPrimaryPaint);
        canvas.drawText(hallMonths + " Month(s)", 280, 400, textPrimaryPaint);
        canvas.drawText(String.format(Locale.US, "%.0f BDT", hallFee), 543, 400, rightTextPrimary);
        canvas.drawLine(40, 413, 555, 413, dividerPaint);

        canvas.drawText("Total Billed Amount", 52, 435, textPrimaryBold);
        canvas.drawText(String.format(Locale.US, "%.0f BDT", grossBill), 543, 435, textRightBold);
        canvas.drawLine(40, 448, 555, 448, dividerPaint);

        canvas.drawText("Previously Paid", 52, 470, textSecondaryPaint);
        Paint rightSecondary = new Paint(textSecondaryPaint);
        rightSecondary.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.format(Locale.US, "%.0f BDT", alreadyPaid), 543, 470, rightSecondary);
        canvas.drawLine(40, 483, 555, 483, dividerPaint);

        Paint highlightBg = new Paint();
        highlightBg.setColor(Color.parseColor("#FFEBEE"));
        canvas.drawRect(40, 492, 555, 526, highlightBg);

        Paint rightRedBold = new Paint(textRedBold);
        rightRedBold.setTextAlign(Paint.Align.RIGHT);

        canvas.drawText("Amount Paid Now", 52, 513, textRedBold);
        canvas.drawText(String.format(Locale.US, "%.0f BDT", paidNow), 543, 513, rightRedBold);

        RectF paymentRect = new RectF(40, 542, 555, 622);
        canvas.drawRoundRect(paymentRect, 8, 8, lightBgPaint);
        canvas.drawRoundRect(paymentRect, 8, 8, borderPaint);

        canvas.drawText("Payment Verification", 52, 564, textRedBold);
        canvas.drawText("Transaction ID: " + payment.getTrxId(), 52, 584, textPrimaryBold);
        canvas.drawText("Payment Method: " + payment.getMethod() + " | Account: " + payment.getAccountNumber(), 52, 604, textPrimaryPaint);

        RectF badgeRect = new RectF(400, 570, 543, 602);
        Paint badgeBg = new Paint();
        badgeBg.setColor(Color.parseColor("#FFCDD2"));
        canvas.drawRoundRect(badgeRect, 6, 6, badgeBg);

        Paint badgeText = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeText.setColor(Color.parseColor("#B71C1C"));
        badgeText.setTextSize(11);
        badgeText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        badgeText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("PAID - SUCCESS", 471, 590, badgeText);

        Paint sigLine = new Paint();
        sigLine.setColor(Color.parseColor("#9E9E9E"));
        sigLine.setStrokeWidth(1);

        canvas.drawLine(52, 720, 200, 720, sigLine);
        canvas.drawText("Student Signature", 80, 735, textSecondaryPaint);

        canvas.drawLine(395, 720, 543, 720, sigLine);
        canvas.drawText("Authorized Signatory", 420, 735, textSecondaryPaint);

        canvas.drawLine(40, 775, 555, 775, borderPaint);
        Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        footerPaint.setColor(Color.parseColor("#757575"));
        footerPaint.setTextSize(9);
        footerPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("This is a computer-generated receipt of Amar Ekushey Hall.", 595 / 2f, 792, footerPaint);

        document.finishPage(page);

        String fileName = "Amar_Ekushey_Hall_Receipt_" + payment.getTrxId() + ".pdf";

        File appDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (appDir != null && !appDir.exists()) {
            appDir.mkdirs();
        }
        File appFile = new File(appDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(appFile)) {
            document.writeTo(fos);
        } catch (IOException e) {
            document.close();
            Log.e("PDF_GEN", "Failed to write local document copy", e);
            Toast.makeText(context, "An unexpected error occurred. Please try again.", Toast.LENGTH_LONG).show();
            return null;
        } finally {
            document.close();
        }

        saveToPublicDownloads(context, appFile, fileName);

        Toast.makeText(context, "Receipt downloaded to Downloads folder", Toast.LENGTH_SHORT).show();

        Uri uri = FileProvider.getUriForFile(context, "com.example.smarthall.fileprovider", appFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent, "Open Amar Ekushey Hall Receipt"));

        return appFile;
    }

    private static void saveToPublicDownloads(Context context, File sourceFile, String fileName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri downloadUri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (downloadUri != null) {
                try (InputStream in = new FileInputStream(sourceFile);
                     OutputStream out = context.getContentResolver().openOutputStream(downloadUri)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                } catch (Exception e) {
                    Log.e("PDF_GEN", "Error copying file to MediaStore.Downloads", e);
                }
            }
        } else {
            File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (publicDir != null) {
                if (!publicDir.exists()) {
                    publicDir.mkdirs();
                }
                File publicFile = new File(publicDir, fileName);
                try (InputStream in = new FileInputStream(sourceFile);
                     OutputStream out = new FileOutputStream(publicFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    MediaScannerConnection.scanFile(context,
                            new String[]{publicFile.getAbsolutePath()},
                            new String[]{"application/pdf"},
                            null);
                } catch (Exception e) {
                    Log.e("PDF_GEN", "Error copying file to public Downloads directory", e);
                }
            }
        }
    }
}
