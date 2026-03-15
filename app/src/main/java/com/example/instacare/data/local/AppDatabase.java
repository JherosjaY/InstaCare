package com.example.instacare.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;

@Database(entities = {ActivityLog.class, User.class, EmergencyContact.class, Guide.class, Hospital.class, Endorsement.class, Notification.class}, version = 20, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract ActivityLogDao activityLogDao();
    public abstract UserDao userDao();
    public abstract EmergencyContactDao emergencyContactDao();
    public abstract GuideDao guideDao();
    public abstract HospitalDao hospitalDao();
    public abstract EndorsementDao endorsementDao();
    public abstract NotificationDao notificationDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "instacare_database")
                            .fallbackToDestructiveMigration()
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    // Seed Database
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        // Guides (35 Total: 5 per category)
                                        List<Guide> guides = new ArrayList<>();
                                        
                                        // --- 1. Bleeding ---
                                        guides.add(new Guide("b1", "Basic Treatment for Cuts", "Bleeding", "Fundamental steps for cleaning and dressing minor cuts.", "Easy", "5 mins", 15400, "https://www.youtube.com/watch?v=9XpJZv_YsGM", "[{\"order\": 1, \"description\": \"Wash your hands thoroughly with clean water and soap before touching the wound to prevent any risk of infection.\"}, {\"order\": 2, \"description\": \"Apply gentle, but firm, pressure to the cut using a clean cloth, tissue, or piece of gauze until the bleeding fully stops. This may take a few minutes.\"}, {\"order\": 3, \"description\": \"Clean the wound gently with cool running water. Avoid getting soap directly into the exposed cut to prevent irritation.\"}, {\"order\": 4, \"description\": \"Cover the wound with a sterile bandage or gauze pad. Change the dressing daily to keep it clean and dry.\"}]", false));
                                        guides.add(new Guide("b2", "Minor Bleeding Care", "Bleeding", "Essential tips for managing common cuts and scrapes.", "Easy", "3 mins", 12100, "https://www.youtube.com/watch?v=nG_F4bqUWKI", "[{\"order\": 1, \"description\": \"Stop the bleeding quickly by elevating the injured area and applying continuous pressure with a sterile dressing.\"}, {\"order\": 2, \"description\": \"Clean the surrounding area carefully with water and mild soap, ensuring you wipe away from the open injury.\"}, {\"order\": 3, \"description\": \"Protect the wound with an appropriate-sized adhesive dressing or medical tape to shield it from bacteria.\"}]", false));
                                        guides.add(new Guide("b3", "Trauma Wound Packing", "Bleeding", "Life-saving technique for deep trauma wounds.", "Hard", "Immediate", 28500, "https://www.youtube.com/watch?v=ZtrCNB2Jm9c", "[{\"order\": 1, \"description\": \"Expose the wound completely by cutting or tearing away any clothing that obscures the point of bleeding.\"}, {\"order\": 2, \"description\": \"Pack the wound tightly and as deep as possible using sterile gauze or a clean cloth until the cavity is completely filled.\"}, {\"order\": 3, \"description\": \"Apply massive, firm body-weight pressure directly over the packed wound using both hands until professional emergency services arrive.\"}]", false));
                                        guides.add(new Guide("b4", "Treating Nose Bleeds", "Bleeding", "Safe steps to stop a nosebleed quickly.", "Easy", "10 mins", 19200, "https://www.youtube.com/watch?v=PmmhxW0vVXA", "[{\"order\": 1, \"description\": \"Have the person sit down and lean forward slightly, allowing blood to drain from the nose rather than down the throat.\"}, {\"order\": 2, \"description\": \"Pinch the soft, fleshy part of the nose just below the bony bridge firmly with your thumb and index finger.\"}, {\"order\": 3, \"description\": \"Hold this pressure continuously for at least 10 full minutes without letting go to check if it has stopped.\"}]", false));
                                        guides.add(new Guide("b5", "Internal Bleeding Signs", "Bleeding", "Recognizing critical signs of internal trauma.", "Hard", "Emergency", 31000, "https://www.youtube.com/watch?v=NxO5LvgqZe0", "[{\"order\": 1, \"description\": \"Check the victim for extreme pain, swelling, rigidness in the abdomen, or large areas of dark purple bruising.\"}, {\"order\": 2, \"description\": \"Keep the patient perfectly still, lying down flat. Do NOT give them anything to eat or drink.\"}, {\"order\": 3, \"description\": \"Call emergency hotlines immediately as internal bleeding requires urgent surgical hospital intervention.\"}]", false));

                                        // --- 2. Cardiac ---
                                        guides.add(new Guide("c1", "Cardiac Arrest Response", "Cardiac", "How to respond to sudden cardiac arrest.", "Hard", "Immediate", 48600, "https://www.youtube.com/watch?v=qvf_74DM880", "[{\"order\": 1, \"description\": \"Check response/breathing.\"}, {\"order\": 2, \"description\": \"Call emergency services.\"}, {\"order\": 3, \"description\": \"Start chest compressions.\"}]", false));
                                        guides.add(new Guide("c2", "Heart Attack Recognition", "Cardiac", "Signs and immediate first aid for heart attacks.", "Medium", "Ongoing", 39500, "https://www.youtube.com/watch?v=6ZGg0zJUFEI", "[{\"order\": 1, \"description\": \"Sit person down.\"}, {\"order\": 2, \"description\": \"Call for help.\"}, {\"order\": 3, \"description\": \"Loosen tight clothing.\"}]", false));
                                        guides.add(new Guide("c3", "Chest Compression Rhythm", "Cardiac", "Mastering the rhythm of 100-120 bpm.", "Medium", "Ongoing", 24200, "https://www.youtube.com/watch?v=2PngCv7NjaI", "[{\"order\": 1, \"description\": \"Hand in center of chest.\"}, {\"order\": 2, \"description\": \"Push hard and fast.\"}]", false));
                                        guides.add(new Guide("c4", "Full CPR Cycle", "Cardiac", "Combining compressions and rescue breaths.", "Hard", "Ongoing", 52000, "https://www.youtube.com/watch?v=TsJ49Np3HS0", "[{\"order\": 1, \"description\": \"30 compressions.\"}, {\"order\": 2, \"description\": \"2 rescue breaths.\"}, {\"order\": 3, \"description\": \"Repeat cycle.\"}]", false));
                                        guides.add(new Guide("c5", "Recovery Position", "Cardiac", "Safe position for unconscious breathing victims.", "Easy", "5 mins", 18500, "https://www.youtube.com/watch?v=Plse2FOkV4Q", "[{\"order\": 1, \"description\": \"Roll onto side.\"}, {\"order\": 2, \"description\": \"Tilt head back slightly.\"}, {\"order\": 3, \"description\": \"Stay with patient.\"}]", false));

                                        // --- 3. Burn ---
                                        guides.add(new Guide("bu1", "Burns and Scalds Treatment", "Burn", "The 'cool, call, cover' method for burns.", "Easy", "20 mins", 22000, "https://www.youtube.com/watch?v=EaJmzB8YgS4", "[{\"order\": 1, \"description\": \"Cool with water for 20m.\"}, {\"order\": 2, \"description\": \"Remove jewelry.\"}, {\"order\": 3, \"description\": \"Cover with cling film.\"}]", false));
                                        guides.add(new Guide("bu2", "General Burn First Aid", "Burn", "Essential tips for managing common burns.", "Easy", "10 mins", 15300, "https://www.youtube.com/watch?v=O1K2ZBePh_E", "[{\"order\": 1, \"description\": \"Cool with running water.\"}, {\"order\": 2, \"description\": \"Don't use ice or creams.\"}, {\"order\": 3, \"description\": \"Cover loosely.\"}]", false));
                                        guides.add(new Guide("bu3", "Identifying Burn Severity", "Burn", "Determining when a burn requires the hospital.", "Medium", "5 mins", 18700, "https://www.youtube.com/watch?v=A8vE62l0p6o", "[{\"order\": 1, \"description\": \"Check for blistering.\"}, {\"order\": 2, \"description\": \"Measure size of burn.\"}, {\"order\": 3, \"description\": \"Check victim's age.\"}]", false));
                                        guides.add(new Guide("bu4", "Chemical Burn Protocol", "Burn", "Handling hazardous chemical injuries safely.", "Hard", "30 mins", 14200, "https://www.youtube.com/watch?v=M9-2f2fO97A", "[{\"order\": 1, \"description\": \"Flush area for 20m.\"}, {\"order\": 2, \"description\": \"Avoid cross-contamination.\"}, {\"order\": 3, \"description\": \"Seek medical help.\"}]", false));
                                        guides.add(new Guide("bu5", "Severe Sunburn Care", "Burn", "Cooling and hydration steps for sunburn.", "Easy", "Ongoing", 11500, "https://www.youtube.com/watch?v=XshF_-B7Gxw", "[{\"order\": 1, \"description\": \"Take a cool bath.\"}, {\"order\": 2, \"description\": \"Drink plenty of water.\"}, {\"order\": 3, \"description\": \"Apply moisturizer.\"}]", false));

                                        // --- 4. Choking ---
                                        guides.add(new Guide("ch1", "Adult Choking Help", "Choking", "Back blows and abdominal thrusts for adults.", "Hard", "Immediate", 25000, "https://www.youtube.com/watch?v=PA9hpOnvtCk", "[{\"order\": 1, \"description\": \"Give 5 back blows.\"}, {\"order\": 2, \"description\": \"Give 5 abdominal thrusts.\"}, {\"order\": 3, \"description\": \"Repeat until clear.\"}]", false));
                                        guides.add(new Guide("ch2", "Infant Choking Recovery", "Choking", "Gentle techniques for babies and infants.", "Hard", "Immediate", 21000, "https://www.youtube.com/watch?v=f9vT8CPr_9U", "[{\"order\": 1, \"description\": \"Support chin/head.\"}, {\"order\": 2, \"description\": \"5 sharp back blows.\"}, {\"order\": 3, \"description\": \"5 chest thrusts.\"}]", false));
                                        guides.add(new Guide("ch3", "Choking Child (1-8)", "Choking", "Assisting a child who cannot breathe.", "Hard", "Immediate", 19200, "https://www.youtube.com/watch?v=T_7_jScl_M4", "[{\"order\": 1, \"description\": \"Encourage coughing.\"}, {\"order\": 2, \"description\": \"5 back blows.\"}, {\"order\": 3, \"description\": \"5 abdominal thrusts.\"}]", false));
                                        guides.add(new Guide("ch4", "Conscious Choking Steps", "Choking", "Protocols for adults and older children.", "Hard", "Immediate", 15400, "https://www.youtube.com/watch?v=YoVIdX3tH0A", "[{\"order\": 1, \"description\": \"Identify the blockage.\"}, {\"order\": 2, \"description\": \"Perform Heimlich.\"}, {\"order\": 3, \"description\": \"Call emergency support.\"}]", false));
                                        guides.add(new Guide("ch5", "Heimlich Overview", "Choking", "Quick reference for recognizing choking signs.", "Medium", "Immediate", 12800, "https://www.youtube.com/watch?v=wXmC-YF-YhU", "[{\"order\": 1, \"description\": \"Check if they can speak.\"}, {\"order\": 2, \"description\": \"Stand behind them.\"}, {\"order\": 3, \"description\": \"Pull in and up.\"}]", false));

                                        // --- 5. Fracture ---
                                        guides.add(new Guide("fr1", "Broken Bone First Aid", "Fracture", "Instructions on immobilizing limbs.", "Medium", "Until help", 18500, "https://www.youtube.com/watch?v=2v8vlXgGX_g", "[{\"order\": 1, \"description\": \"Support the injury.\"}, {\"order\": 2, \"description\": \"Apply cold compress.\"}, {\"order\": 3, \"description\": \"Keep perfectly still.\"}]", false));
                                        guides.add(new Guide("fr2", "Identifying Fractures", "Fracture", "Keeping the injury supported and immobilized.", "Easy", "Immediate", 14300, "https://www.youtube.com/watch?v=gT-o6n2DqGo", "[{\"order\": 1, \"description\": \"Check for swelling.\"}, {\"order\": 2, \"description\": \"Support with padding.\"}, {\"order\": 3, \"description\": \"Avoid movement.\"}]", false));
                                        guides.add(new Guide("fr3", "Making an Arm Sling", "Fracture", "Using a triangular bandage correctly.", "Medium", "10 mins", 11200, "https://www.youtube.com/watch?v=8I2_vH7Iu5o", "[{\"order\": 1, \"description\": \"Slide bandage under arm.\"}, {\"order\": 2, \"description\": \"Tie at the shoulder.\"}, {\"order\": 3, \"description\": \"Secure with pin.\"}]", false));
                                        guides.add(new Guide("fr4", "Leg Injury Support", "Fracture", "Supporting lower limb bone injuries.", "Medium", "15 mins", 15400, "https://www.youtube.com/watch?v=LSTGq_s2m-s", "[{\"order\": 1, \"description\": \"Pad between legs.\"}, {\"order\": 2, \"description\": \"Tie legs together.\"}, {\"order\": 3, \"description\": \"Wait for ambulance.\"}]", false));
                                        guides.add(new Guide("fr5", "Managing Dislocations", "Fracture", "Differences from breaks and immobilization.", "Medium", "20 mins", 12900, "https://www.youtube.com/watch?v=n6W_D-JmXYM", "[{\"order\": 1, \"description\": \"Comfort the victim.\"}, {\"order\": 2, \"description\": \"Support in position.\"}, {\"order\": 3, \"description\": \"Seek hospital care.\"}]", false));

                                        // --- 6. Poisoning ---
                                        guides.add(new Guide("po1", "Swallowed Poison Care", "Poisoning", "Immediate actions for toxic ingestion.", "Hard", "Immediate", 22500, "https://www.youtube.com/watch?v=LIsmH8_R0rY", "[{\"order\": 1, \"description\": \"Identify the poison.\"}, {\"order\": 2, \"description\": \"Call poison control.\"}, {\"order\": 3, \"description\": \"Keep sample for help.\"}]", false));
                                        guides.add(new Guide("po2", "Alcohol Poisoning Help", "Poisoning", "Using the recovery position for over-intoxication.", "Hard", "Immediate", 19800, "https://www.youtube.com/watch?v=q6t8nO71fJ0", "[{\"order\": 1, \"description\": \"Keep them warm.\"}, {\"order\": 2, \"description\": \"Roll onto their side.\"}, {\"order\": 3, \"description\": \"Stay with them.\"}]", false));
                                        guides.add(new Guide("po3", "Drug Overdose Response", "Poisoning", "Managing drug reactions and vitals.", "Hard", "Immediate", 28400, "https://www.youtube.com/watch?v=B657V9mF7-0", "[{\"order\": 1, \"description\": \"Call emergency hotlines immediately.\"}, {\"order\": 2, \"description\": \"Monitor breathing.\"}, {\"order\": 3, \"description\": \"Check for meds nearby.\"}]", false));
                                        guides.add(new Guide("po4", "Corrosive Poisoning", "Poisoning", "Hazards of corrosive ingestion.", "Hard", "Immediate", 17300, "https://www.youtube.com/watch?v=uD9lO_V8f1M", "[{\"order\": 1, \"description\": \"Do NOT induce vomit.\"}, {\"order\": 2, \"description\": \"Rinse lips/mouth.\"}, {\"order\": 3, \"description\": \"Call for ambulance.\"}]", false));
                                        guides.add(new Guide("po5", "Identifying Toxic Signs", "Poisoning", "Expert advice on identifying toxicity.", "Medium", "5 mins", 13200, "https://www.youtube.com/watch?v=mE9mI823yX8", "[{\"order\": 1, \"description\": \"Check pupils/breath.\"}, {\"order\": 2, \"description\": \"Check for rashes.\"}, {\"order\": 3, \"description\": \"Note the time found.\"}]", false));

                                        // --- 7. Wounds ---
                                        guides.add(new Guide("wo1", "Cleaning Minor Wounds", "Wounds", "Cleaning and dressing small cuts.", "Easy", "5 mins", 11500, "https://www.youtube.com/watch?v=W6O_U9o1Vv0", "[{\"order\": 1, \"description\": \"Clean with water.\"}, {\"order\": 2, \"description\": \"Dry carefully.\"}, {\"order\": 3, \"description\": \"Apply a plaster.\"}]", false));
                                        guides.add(new Guide("wo2", "Major Wound Compression", "Wounds", "Techniques for heavy bleeding control.", "Hard", "Immediate", 24200, "https://www.youtube.com/watch?v=nxXW_u_TizQ", "[{\"order\": 1, \"description\": \"Press on the wound.\"}, {\"order\": 2, \"description\": \"Apply bulky dressing.\"}, {\"order\": 3, \"description\": \"Seek urgent care.\"}]", false));
                                        guides.add(new Guide("wo3", "Bleeding Cut First Aid", "Wounds", "Stop bleeding and protect the area.", "Easy", "5 mins", 14300, "https://www.youtube.com/watch?v=E-0_bA5_Cno", "[{\"order\": 1, \"description\": \"Wash your hands.\"}, {\"order\": 2, \"description\": \"Elevate the limb.\"}, {\"order\": 3, \"description\": \"Apply firm pressure.\"}]", false));
                                        guides.add(new Guide("wo4", "Head Wound Assessment", "Wounds", "Monitoring for concussion or trauma.", "Medium", "10 mins", 18700, "https://www.youtube.com/watch?v=pDra9nE5W1Y", "[{\"order\": 1, \"description\": \"Control Scalp bleeding.\"}, {\"order\": 2, \"description\": \"Avoid movement.\"}, {\"order\": 3, \"description\": \"Watch for vomit/dizzy.\"}]", false));
                                        guides.add(new Guide("wo5", "Eye Injury Treatment", "Wounds", "Flushing foreign objects safely.", "Medium", "10 mins", 15300, "https://www.youtube.com/watch?v=jW_nK6-Ewyo", "[{\"order\": 1, \"description\": \"Tilt head to side.\"}, {\"order\": 2, \"description\": \"Wash with tap water.\"}, {\"order\": 3, \"description\": \"Don't rub the eye.\"}]", false));

                                        getDatabase(context).guideDao().insertAll(guides);

                                        // Hospitals (2025 Updated Dataset - Philippines)
                                        List<Hospital> hospitals = new ArrayList<>();
                                        
                                        // Valencia City (Current Focus)
                                        hospitals.add(new Hospital("v1", "Valencia Medical Hospital", "0.8 miles", "Sayre Highway, Valencia City", 
                                            "+63 88 828 1234", true, "Hospital", "2025 Updated: ER, Trauma, Pediatrics", 7.9120, 125.0950,
                                            "hosp_valencia", "Available"));
                                        hospitals.add(new Hospital("v2", "Adventist Medical Center", "1.2 miles", "Hagkol, Valencia City", 
                                            "+63 88 828 5678", true, "Hospital", "2025 Updated: ER, Surgery, Cardiology", 7.9050, 125.0900,
                                            "hosp_adventist", "Full"));
                                        hospitals.add(new Hospital("v3", "Valencia Sanitarium", "3.5 miles", "Purok 10, Valencia City", 
                                            "+63 88 828 9111", true, "Clinic", "2025 Updated: Outpatient, Ob-Gyn", 7.9150, 125.0880,
                                            "hosp_valencia", "Moderate"));

                                        // Manila (Capital Region)
                                        hospitals.add(new Hospital("m1", "Philippine General Hospital", "N/A", "Taft Ave, Ermita, Manila", 
                                            "+63 2 8554 8400", true, "Hospital", "2025 Updated: Tertiary Care, ER", 14.5786, 120.9850,
                                            "hosp_spmc", "Available"));
                                        hospitals.add(new Hospital("m2", "St. Luke's Medical Center", "N/A", "BGC, Taguig City", 
                                            "+63 2 8789 7700", true, "Hospital", "2025 Updated: Premium Care, ICU", 14.5550, 121.0490,
                                            "hosp_st_lukes", "Available"));

                                        // Cebu City (Visayas)
                                        hospitals.add(new Hospital("c1", "Cebu Doctors' Hospital", "N/A", "Osmeña Blvd, Cebu City", 
                                            "+63 32 255 5555", true, "Hospital", "2025 Updated: Comprehensive Care", 10.3120, 123.8930,
                                            "hosp_cebu_doctors", "Full"));

                                        // Davao City (Mindanao)
                                        hospitals.add(new Hospital("d1", "Southern Philippines Med Center", "N/A", "JP Laurel Ave, Davao City", 
                                            "+63 82 227 2731", true, "Hospital", "2025 Updated: Mega Hospital", 7.0910, 125.6200,
                                            "hosp_spmc", "Available"));

                                        // Baguio City (North Luzon)
                                        hospitals.add(new Hospital("b1", "Baguio General Hospital", "N/A", "Gov. Pack Rd, Baguio City", 
                                            "+63 74 442 4235", true, "Hospital", "2025 Updated: North Luzon Hub", 16.4023, 120.5960,
                                            "hosp_st_lukes", "Moderate"));

                                        // Zamboanga
                                        hospitals.add(new Hospital("z1", "Zamboanga City Med Center", "N/A", "Dr. Evangelista St, Zamboanga", 
                                            "+63 62 991 0573", true, "Hospital", "2025 Updated: Zambo Peninsula Hub", 6.9180, 122.0780,
                                            "hosp_adventist", "Available"));

                                        getDatabase(context).hospitalDao().insertAll(hospitals);
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

