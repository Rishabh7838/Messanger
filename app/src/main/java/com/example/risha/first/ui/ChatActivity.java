package com.example.risha.first.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

//import com.example.risha.first.SentimentsFragments.ApiFragment;
//import com.example.risha.first.SentimentsFragments.SentimentInfo;
import com.example.risha.first.MainActivity;
import com.example.risha.first.model.User;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
//import com.google.api.services.language.v1.CloudNaturalLanguage;
//import com.google.api.services.language.v1.CloudNaturalLanguageRequestInitializer;
//import com.google.api.services.language.v1.model.AnnotateTextRequest;
//import com.google.api.services.language.v1.model.AnnotateTextResponse;
//import com.google.api.services.language.v1.model.Document;
//import com.google.api.services.language.v1.model.Features;
import com.google.api.services.language.v1beta2.CloudNaturalLanguage;
import com.google.api.services.language.v1beta2.CloudNaturalLanguageRequestInitializer;
import com.google.api.services.language.v1beta2.model.AnnotateTextRequest;
import com.google.api.services.language.v1beta2.model.AnnotateTextResponse;
import com.google.api.services.language.v1beta2.model.Document;
import com.google.api.services.language.v1beta2.model.Features;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.risha.first.R;
import com.example.risha.first.data.SharedPreferenceHelper;
import com.example.risha.first.data.StaticConfig;
import com.example.risha.first.model.Consersation;
import com.example.risha.first.model.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import cn.pedant.SweetAlert.SweetAlertDialog;
import de.hdodenhof.circleimageview.CircleImageView;


public class ChatActivity extends AppCompatActivity implements View.OnClickListener{
    private RecyclerView recyclerChat;
    public static final int VIEW_TYPE_USER_MESSAGE = 0;
    public static final int VIEW_TYPE_FRIEND_MESSAGE = 1;
    private ListMessageAdapter adapter;
    private String roomId,rec_id="";
    private static  String rec_lang="",send_lang="",send_name="";
    private ArrayList<CharSequence> idFriend;
    private Consersation consersation;
    private ImageButton btnSend;
    private EditText editWriteMessage;
    private LinearLayoutManager linearLayoutManager;
    public static HashMap<String, Bitmap> bitmapAvataFriend;
    public Bitmap bitmapAvataUser;
    private CloudNaturalLanguage naturalLanguageService;
    private float sentiment = 0;
    //private SweetAlertDialog warning_dialog;
    private String sender ="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intentData = getIntent();
        idFriend = intentData.getCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID);
        roomId = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID);
        sender = intentData.getStringExtra("Sender");
        //rec_lang = intentData.getStringExtra("Receiver_Language");
        rec_id = intentData.getStringExtra("Receiver_ID");
        setRecieverLanguage();

        //NLP Processing here

//         warning_dialog = new SweetAlertDialog(this,SweetAlertDialog.WARNING_TYPE);

        String nameFriend = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND);

        consersation = new Consersation();
        btnSend = (ImageButton) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(this);

        String base64AvataUser = SharedPreferenceHelper.getInstance(this).getUserInfo().avata;
        if (!base64AvataUser.equals(StaticConfig.STR_DEFAULT_BASE64)) {
            byte[] decodedString = Base64.decode(base64AvataUser, Base64.DEFAULT);
            bitmapAvataUser = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        } else {
            bitmapAvataUser = null;
        }

        editWriteMessage = (EditText) findViewById(R.id.editWriteMessage);
        if (idFriend != null && nameFriend != null) {
            getSupportActionBar().setTitle(nameFriend);
            linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            recyclerChat = (RecyclerView) findViewById(R.id.recyclerChat);
            recyclerChat.setLayoutManager(linearLayoutManager);
            if(sender==null)
                sender = "A";
            adapter = new ListMessageAdapter(this, consersation, bitmapAvataFriend, bitmapAvataUser,sender);
            FirebaseDatabase.getInstance().getReference().child("message/" + roomId).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    if (dataSnapshot.getValue() != null) {
                        HashMap mapMessage = (HashMap) dataSnapshot.getValue();
                        Message newMessage = new Message();
                        newMessage.idSender = (String) mapMessage.get("idSender");
                        newMessage.idReceiver = (String) mapMessage.get("idReceiver");
                        newMessage.text = (String) mapMessage.get("text");
                        newMessage.orignal_text = (String) mapMessage.get("orignal_text");
                        newMessage.sentiment_score = (String) mapMessage.get("sentiment_score");
                        newMessage.timestamp = (long) mapMessage.get("timestamp");
                        newMessage.sender_name = (String) mapMessage.get("sender_name");
                        consersation.getListMessageData().add(newMessage);
                        adapter.notifyDataSetChanged();
                        linearLayoutManager.scrollToPosition(consersation.getListMessageData().size() - 1);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            recyclerChat.setAdapter(adapter);
        }
    }

    private void setRecieverLanguage() {

        FirebaseDatabase.getInstance().getReference().child("user/" + rec_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    rec_lang = dataSnapshot.child("Native_Language").getValue().toString();
                  //  Toast.makeText(ChatActivity.this, rec_lang, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });



        FirebaseDatabase.getInstance().getReference().child("user/" + StaticConfig.UID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    send_lang = dataSnapshot.child("Native_Language").getValue().toString();
                    send_name = dataSnapshot.child("name").getValue().toString();
                    //  Toast.makeText(ChatActivity.this, rec_lang, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            Intent result = new Intent();
            result.putExtra("idFriend", idFriend.get(0));
            setResult(RESULT_OK, result);
            this.finish();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        result.putExtra("idFriend", idFriend.get(0));
        setResult(RESULT_OK, result);
        this.finish();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnSend) {
            String content = editWriteMessage.getText().toString().trim();
            if(content.length()>0)
                editWriteMessage.setText("");
            if(!rec_lang.isEmpty()){
//                for(Friend friend: FriendsFragment.dataListFriend.getListFriend()){
//                    if(roomId == friend.idRoom)
//                    {

                        Translate(content,rec_lang);
//                    }
//                }
            }
            else
                Translate(content,"en");


        }
    }

    private void Translate(final String msg, String language) {
//        String temp = "";
//       // if(sender!=null)
//            temp = "G";
//        else
//            temp = "A";




        YourTask yourTask = new YourTask(msg, language, ChatActivity.this, roomId,sender);
        yourTask.execute();

//
//        AsyncTask.execute(new Runnable() {
//
//            @Override
//            public void run() {
//
//
//                TranslateOptions options = TranslateOptions.newBuilder()
//                        .setApiKey(StaticConfig.API_KEY)
//                        .build();
//                Translate translate = options.getService();
//                String content = "";
//                final Translation translation =
//                        translate.translate(msg,
//                                Translate.TranslateOption.targetLanguage(language));
//                content = translation.getTranslatedText();
//
//                //Sentiment analysis
//
//                naturalLanguageService =
//                        new CloudNaturalLanguage.Builder(
//                                AndroidHttp.newCompatibleTransport(),
//                                new AndroidJsonFactory(),
//                                null
//                        ).setCloudNaturalLanguageRequestInitializer(
//                                new CloudNaturalLanguageRequestInitializer(StaticConfig.API_KEY)
//                        ).build();
//
//                String transcript = msg;
//
//                SharedPreferenceHelper prefHelper = SharedPreferenceHelper.getInstance(ChatActivity.this);
//                User user = prefHelper.getUserInfo();
//
//                Document document = new Document();
//                document.setType("PLAIN_TEXT");
//                document.setLanguage(user.Native_Language);
//                document.setContent(transcript);
//
//                Features features = new Features();
//                features.setExtractDocumentSentiment(true);
//
//                final AnnotateTextRequest request = new AnnotateTextRequest();
//                request.setDocument(document);
//                request.setFeatures(features);
//
//                try {
//                    AnnotateTextResponse response =
//                            naturalLanguageService.documents()
//                                    .annotateText(request).execute();
//
//                    sentiment = response.getDocumentSentiment().getScore();
//
//
//                    Log.e("senti", msg + "= " + sentiment);
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//
//
//
//                if (sentiment <= -0.3) {
//                    final String finalContent = content;
//                    AlertDialog.Builder builder;
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                        builder = new AlertDialog.Builder(ChatActivity.this, android.R.style.Theme_Material_Dialog_Alert);
//                    } else {
//                        builder = new AlertDialog.Builder(ChatActivity.this);
//                    }
//                    builder.setTitle("This message contains violent words :(")
//                            .setMessage("Are you sure you want to send this ?")
//                            .setPositiveButton("Send It!", new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int which) {
//
//                                    Message newMessage = new Message();
//                                    newMessage.text = finalContent;
//                                    newMessage.orignal_text = msg;
//                                    newMessage.idSender = StaticConfig.UID;
//                                    newMessage.idReceiver = roomId;
//                                    newMessage.sentiment_score = Float.toString(sentiment);
//                                    newMessage.timestamp = System.currentTimeMillis();
//                                    FirebaseDatabase.getInstance().getReference().child("message/" + roomId).push().setValue(newMessage);
//                                }
//                            })
//                            .setNegativeButton("Don't Send!", new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int which) {
//                                    dialog.dismiss();
//                                }
//                            })
//                            .setIcon(android.R.drawable.ic_dialog_alert)
//                            .show();
//                } else {
//                    if (content.length() > 0) {
//
//                        Message newMessage = new Message();
//                        newMessage.text = content;
//                        newMessage.orignal_text = msg;
//                        newMessage.idSender = StaticConfig.UID;
//                        newMessage.idReceiver = roomId;
//                        newMessage.sentiment_score = Float.toString(sentiment);
//                        newMessage.timestamp = System.currentTimeMillis();
//                        FirebaseDatabase.getInstance().getReference().child("message/" + roomId).push().setValue(newMessage);
//                    }
//                }
//            }
//        });

        //return msg;
    }

    private static class YourTask extends AsyncTask<String, Void, String>
    {
        String msg,language;
        Context context = null;
        private CloudNaturalLanguage naturalLanguageService;
        private float sentiment = 0;
        private String roomId= "";
        private String sender = "";
        String content = "";
        private int f = -1;
        AlertDialog.Builder builder;
        public YourTask(String msg, String language, Context context, String roomID,String sender) {
            this.msg = msg;
            this.language = language;
            this.context = context;
            this.sender = sender;
            roomId = roomID;
            //this.naturalLanguageService = naturalLanguageService;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
                builder = new AlertDialog.Builder(context);
        }

        @Override
        protected String doInBackground(String... urls)
        {
            SharedPreferenceHelper prefHelper = SharedPreferenceHelper.getInstance(context);
            User user = prefHelper.getUserInfo();

            if(sender.equals("G")) {
                if(!send_lang.equals("en")) {
                    language = "en";
                    f=1;
                }
                else f=0;
            }
            else if(sender.equals("A")) {
                if(send_lang.equals(language)) {
                    f=0;
                }
                else f=1;
            }
            if(f==1 || f==-1) {
                TranslateOptions options = TranslateOptions.newBuilder()
                        .setApiKey(StaticConfig.API_KEY)
                        .build();
                Translate translate = options.getService();

                final Translation translation =
                        translate.translate(msg,
                                Translate.TranslateOption.targetLanguage(language));
                content = translation.getTranslatedText();
            }
            else
                content = msg;

            //Sentiment analysis

            naturalLanguageService =
                    new CloudNaturalLanguage.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new AndroidJsonFactory(),
                            null
                    ).setCloudNaturalLanguageRequestInitializer(
                            new CloudNaturalLanguageRequestInitializer(StaticConfig.API_KEY)
                    ).build();

            String transcript = msg;


            Document document = new Document();
            document.setType("PLAIN_TEXT");
            document.setLanguage(send_lang);
            document.setContent(transcript);

            Features features = new Features();
            features.setExtractDocumentSentiment(true);

            final AnnotateTextRequest request = new AnnotateTextRequest();
            request.setDocument(document);
            request.setFeatures(features);

            try {
                AnnotateTextResponse response =
                        naturalLanguageService.documents()
                                .annotateText(request).execute();

                sentiment = response.getDocumentSentiment().getScore();


                Log.e("senti", msg + "= " + sentiment);

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (sentiment <= -0.3) {
                final String finalContent = content;

                builder.setTitle("This message contains violent words :(")
                        .setMessage("Are you sure you want to send this ?")
                        .setPositiveButton("Don't Send!", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("Send It!", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                Message newMessage = new Message();
                                if(sender.equals("G")) {
                                    newMessage.orignal_text = finalContent;
                                }
                                else
                                    newMessage.orignal_text = msg;

                                newMessage.text = finalContent;
                                newMessage.idSender = StaticConfig.UID;
                                newMessage.idReceiver = roomId;
                                newMessage.sentiment_score = Float.toString(sentiment);
                                newMessage.timestamp = System.currentTimeMillis();
                                newMessage.sender_name = send_name;
                                FirebaseDatabase.getInstance().getReference().child("message/" + roomId).push().setValue(newMessage);
                            }
                        })
                        .setIcon(R.drawable.ic_alert_message_24dp)
                        .show();
            } else {
                if (content.length() > 0) {

                    Message newMessage = new Message();
                    if(sender.equals("G")) {
                        newMessage.orignal_text = content;
                    }
                    else
                        newMessage.orignal_text = msg;

                    newMessage.text = content;
                    newMessage.idSender = StaticConfig.UID;
                    newMessage.idReceiver = roomId;
                    newMessage.sentiment_score = Float.toString(sentiment);
                    newMessage.timestamp = System.currentTimeMillis();
                    newMessage.sender_name = send_name;
                    FirebaseDatabase.getInstance().getReference().child("message/" + roomId).push().setValue(newMessage);
                }
            }
        }
    }
}



class ListMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private Consersation consersation;
    private HashMap<String, Bitmap> bitmapAvata;
    private HashMap<String, DatabaseReference> bitmapAvataDB;
    private Bitmap bitmapAvataUser;

    private float score=0;
    private int mColorPositive;
    private int mColorNeutral;
    private int mColorNegative;
    private String sender="";


    public ListMessageAdapter(Context context, Consersation consersation, HashMap<String, Bitmap> bitmapAvata, Bitmap bitmapAvataUser,String sender) {
        this.context = context;
        this.consersation = consersation;
        this.bitmapAvata = bitmapAvata;
        this.bitmapAvataUser = bitmapAvataUser;
        this.sender = sender;
        bitmapAvataDB = new HashMap<>();
        final Resources resources = context.getResources();
        final Resources.Theme theme = context.getTheme();
        mColorPositive = ResourcesCompat.getColor(resources,R.color.score_positive,theme);
        mColorNeutral = ResourcesCompat.getColor(resources, R.color.score_neutral, theme);
        mColorNegative = ResourcesCompat.getColor(resources, R.color.score_negative, theme);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ChatActivity.VIEW_TYPE_FRIEND_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_friend, parent, false);
            return new ItemMessageFriendHolder(view);
        } else if (viewType == ChatActivity.VIEW_TYPE_USER_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_user, parent, false);
            return new ItemMessageUserHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemMessageFriendHolder) {
            //setSentiments(consersation.getListMessageData().get(position).text,((ItemMessageFriendHolder) holder).txtContent);
            score = Float.parseFloat(consersation.getListMessageData().get(position).sentiment_score);
            if(sender.equals("G"))
                ((ItemMessageFriendHolder) holder).Name.setText(consersation.getListMessageData().get(position).sender_name);
            else
                ((ItemMessageFriendHolder) holder).Name.setVisibility(View.GONE);
            ((ItemMessageFriendHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).text);
            if (score > 0.0) {
                ((ItemMessageFriendHolder) holder).txtContent.setBackgroundColor(mColorPositive);
            } else if (score < 0.0) {
                ((ItemMessageFriendHolder) holder).txtContent.setBackgroundColor(mColorNegative);
            } else {
                ((ItemMessageFriendHolder) holder).txtContent.setBackgroundColor(mColorNeutral);
            }
            Bitmap currentAvata = bitmapAvata.get(consersation.getListMessageData().get(position).idSender);
            if (currentAvata != null) {
                ((ItemMessageFriendHolder) holder).avata.setImageBitmap(currentAvata);
            } else {
                final String id = consersation.getListMessageData().get(position).idSender;
                if(bitmapAvataDB.get(id) == null){
                    bitmapAvataDB.put(id, FirebaseDatabase.getInstance().getReference().child("user/" + id + "/avata"));
                    bitmapAvataDB.get(id).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                String avataStr = (String) dataSnapshot.getValue();
                                if(!avataStr.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                                    byte[] decodedString = Base64.decode(avataStr, Base64.DEFAULT);
                                    ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                                }else{
                                    ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeResource(context.getResources(), R.drawable.default_avata));
                                }
                                notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }
        } else if (holder instanceof ItemMessageUserHolder) {
            //setSentiments(consersation.getListMessageData().get(position).orignal_text, ((ItemMessageUserHolder) holder).txtContent);
            ((ItemMessageUserHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).orignal_text);
            score = Float.parseFloat(consersation.getListMessageData().get(position).sentiment_score);
            //((ItemMessageUserHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).text);
            if (score > 0.0) {
                ((ItemMessageUserHolder) holder).txtContent.setBackgroundColor(mColorPositive);
            } else if (score < 0.0) {
                ((ItemMessageUserHolder) holder).txtContent.setBackgroundColor(mColorNegative);
            } else {
                ((ItemMessageUserHolder) holder).txtContent.setBackgroundColor(mColorNeutral);
            }
            if (bitmapAvataUser != null) {
                ((ItemMessageUserHolder) holder).avata.setImageBitmap(bitmapAvataUser);
            }
        }
    }





    @Override
    public int getItemViewType(int position) {
        return consersation.getListMessageData().get(position).idSender.equals(StaticConfig.UID) ? ChatActivity.VIEW_TYPE_USER_MESSAGE : ChatActivity.VIEW_TYPE_FRIEND_MESSAGE;
    }

    @Override
    public int getItemCount() {
        return consersation.getListMessageData().size();
    }
}

class ItemMessageUserHolder extends RecyclerView.ViewHolder {
    public TextView txtContent;
    public CircleImageView avata;

    public ItemMessageUserHolder(View itemView) {
        super(itemView);
        txtContent = (TextView) itemView.findViewById(R.id.textContentUser);
        avata = (CircleImageView) itemView.findViewById(R.id.imageView2);
    }
}

class ItemMessageFriendHolder extends RecyclerView.ViewHolder {
    public TextView txtContent;
    public CircleImageView avata;
    public TextView Name;
    public ItemMessageFriendHolder(View itemView) {
        super(itemView);
        txtContent = itemView.findViewById(R.id.textContentFriend);
        avata =  itemView.findViewById(R.id.imageView3);
        Name = itemView.findViewById(R.id.recNameTV);
    }
}
