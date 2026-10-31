package com.example.smarthall.util;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BillingEngine {

    public static final String BILLING_START_DATE = "2026-10-01";
    public static final double MEAL_RATE_PER_DAY = 65.0;
    public static final double MONTHLY_HALL_FEE = 250.0;

    public static class BillResult implements Serializable {
        private final String roll;
        private final String startDate;
        private final String targetDate;
        private final int activeMealDays;
        private final double mealBill;
        private final int hallMonths;
        private final double hallFee;
        private final double grossBill;
        private final double alreadyPaid;
        private final double netUnpaidAmount;

        public BillResult(String roll, String startDate, String targetDate,
                          int activeMealDays, double mealBill, int hallMonths,
                          double hallFee, double grossBill, double alreadyPaid,
                          double netUnpaidAmount) {
            this.roll = roll;
            this.startDate = startDate;
            this.targetDate = targetDate;
            this.activeMealDays = activeMealDays;
            this.mealBill = mealBill;
            this.hallMonths = hallMonths;
            this.hallFee = hallFee;
            this.grossBill = grossBill;
            this.alreadyPaid = alreadyPaid;
            this.netUnpaidAmount = netUnpaidAmount;
        }

        public String getRoll() { return roll; }
        public String getStartDate() { return startDate; }
        public String getTargetDate() { return targetDate; }
        public int getActiveMealDays() { return activeMealDays; }
        public double getMealBill() { return mealBill; }
        public int getHallMonths() { return hallMonths; }
        public double getHallFee() { return hallFee; }
        public double getGrossBill() { return grossBill; }
        public double getAlreadyPaid() { return alreadyPaid; }
        public double getNetUnpaidAmount() { return netUnpaidAmount; }
    }

    public interface OnBillCalculatedListener {
        void onBillCalculated(BillResult result);
        void onError(Exception e);
    }

    public static void calculateBillUpToDate(String roll, String targetDateStr, OnBillCalculatedListener listener) {
        if (roll == null || roll.trim().isEmpty()) {
            if (listener != null) {
                listener.onError(new IllegalArgumentException("Roll number cannot be empty"));
            }
            return;
        }

        String safeTargetDate = targetDateStr;
        if (safeTargetDate == null || safeTargetDate.compareTo(BILLING_START_DATE) < 0) {
            safeTargetDate = BILLING_START_DATE;
        }

        final String finalTargetDate = safeTargetDate;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("hall_schedule")
                .whereGreaterThanOrEqualTo("date", BILLING_START_DATE)
                .whereLessThanOrEqualTo("date", finalTargetDate)
                .get()
                .addOnSuccessListener(scheduleSnapshots -> {
                    Map<String, String> hallScheduleMap = new HashMap<>();
                    if (scheduleSnapshots != null) {
                        for (DocumentSnapshot doc : scheduleSnapshots.getDocuments()) {
                            String d = doc.getString("date");
                            String s = doc.getString("status");
                            if (d != null && s != null) {
                                hallScheduleMap.put(d, s);
                            }
                        }
                    }

                    db.collection("meals")
                            .whereEqualTo("roll", roll)
                            .get()
                            .addOnSuccessListener(mealsSnapshots -> {
                                Map<String, String> mealsMap = new HashMap<>();
                                if (mealsSnapshots != null) {
                                    for (DocumentSnapshot doc : mealsSnapshots.getDocuments()) {
                                        String d = doc.getString("date");
                                        String s = doc.getString("status");
                                        if (d != null && s != null) {
                                            mealsMap.put(d, s);
                                        }
                                    }
                                }

                                db.collection("payments")
                                        .whereEqualTo("roll", roll)
                                        .get()
                                        .addOnSuccessListener(paymentsSnapshots -> {
                                            double alreadyPaid = 0.0;
                                            if (paymentsSnapshots != null) {
                                                for (DocumentSnapshot doc : paymentsSnapshots.getDocuments()) {
                                                    String status = doc.getString("status");
                                                    if ("SUCCESS".equalsIgnoreCase(status) || status == null) {
                                                        Double amt = doc.getDouble("amount");
                                                        if (amt != null) {
                                                            alreadyPaid += amt;
                                                        }
                                                    }
                                                }
                                            }

                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                            Calendar current = Calendar.getInstance();
                                            Calendar targetCal = Calendar.getInstance();

                                            try {
                                                Date startDate = sdf.parse(BILLING_START_DATE);
                                                Date targetDate = sdf.parse(finalTargetDate);
                                                if (startDate != null) current.setTime(startDate);
                                                if (targetDate != null) targetCal.setTime(targetDate);
                                            } catch (Exception ignored) {
                                            }

                                            current.set(Calendar.HOUR_OF_DAY, 0);
                                            current.set(Calendar.MINUTE, 0);
                                            current.set(Calendar.SECOND, 0);
                                            current.set(Calendar.MILLISECOND, 0);

                                            targetCal.set(Calendar.HOUR_OF_DAY, 0);
                                            targetCal.set(Calendar.MINUTE, 0);
                                            targetCal.set(Calendar.SECOND, 0);
                                            targetCal.set(Calendar.MILLISECOND, 0);

                                            int activeMealDays = 0;
                                            while (!current.after(targetCal)) {
                                                String dStr = sdf.format(current.getTime());
                                                String scheduleStatus = hallScheduleMap.get(dStr);
                                                boolean isClosed = "MEAL_CLOSE".equalsIgnoreCase(scheduleStatus)
                                                        || "HALL_CLOSE".equalsIgnoreCase(scheduleStatus);

                                                if (!isClosed) {
                                                    String mealStatus = mealsMap.get(dStr);
                                                    boolean isOff = "OFF".equalsIgnoreCase(mealStatus);
                                                    if (!isOff) {
                                                        activeMealDays++;
                                                    }
                                                }
                                                current.add(Calendar.DAY_OF_MONTH, 1);
                                            }

                                            int startYear = 2026;
                                            int startMonth = 10;
                                            int targetYear = targetCal.get(Calendar.YEAR);
                                            int targetMonth = targetCal.get(Calendar.MONTH) + 1;
                                            int hallMonths = (targetYear - startYear) * 12 + (targetMonth - startMonth) + 1;
                                            if (hallMonths < 1) {
                                                hallMonths = 1;
                                            }

                                            double mealBill = activeMealDays * MEAL_RATE_PER_DAY;
                                            double hallFee = hallMonths * MONTHLY_HALL_FEE;
                                            double grossBill = mealBill + hallFee;
                                            double netUnpaidAmount = Math.max(0.0, grossBill - alreadyPaid);

                                            BillResult result = new BillResult(
                                                    roll,
                                                    BILLING_START_DATE,
                                                    finalTargetDate,
                                                    activeMealDays,
                                                    mealBill,
                                                    hallMonths,
                                                    hallFee,
                                                    grossBill,
                                                    alreadyPaid,
                                                    netUnpaidAmount
                                            );

                                            if (listener != null) {
                                                listener.onBillCalculated(result);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            if (listener != null) listener.onError(e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                if (listener != null) listener.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e);
                });
    }
}
