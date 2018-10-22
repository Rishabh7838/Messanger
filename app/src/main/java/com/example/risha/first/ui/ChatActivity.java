package com.example.risha.first.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
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

import de.hdodenhof.circleimageview.CircleImageView;


public class ChatActivity extends AppCompatActivity implements View.OnClickListener{
    private RecyclerView recyclerChat;
    public static final int VIEW_TYPE_USER_MESSAGE = 0;
    public static final int VIEW_TYPE_FRIEND_MESSAGE = 1;
    private ListMessageAdapter adapter;
    private String roomId,rec_id="";
    private static  String rec_lang="";
    private ArrayList<CharSequence> idFriend;
    private Consersation consersation;
    private ImageButton btnSend;
    private EditText editWriteMessage;
    private LinearLayoutManager linearLayoutManager;
    public static HashMap<String, Bitmap> bitmapAvataFriend;
    public Bitmap bitmapAvataUser;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intentData = getIntent();
        idFriend = intentData.getCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID);
        roomId = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID);
        //rec_lang = intentData.getStringExtra("Receiver_Language");
        rec_id = intentData.getStringExtra("Receiver_ID");
        setRecieverLanguage();

        //NLP Processing here



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
            adapter = new ListMessageAdapter(this, consersation, bitmapAvataFriend, bitmapAvataUser);
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
                        newMessage.timestamp = (long) mapMessage.get("timestamp");
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

    private void Translate(final String msg,final String language) {



        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {



                TranslateOptions options = TranslateOptions.newBuilder()
                        .setApiKey(StaticConfig.API_KEY)
                        .build();
                 Translate translate = options.getService();
                String content = "";
                final Translation translation =
                        translate.translate(msg,
                                Translate.TranslateOption.targetLanguage(language));
                content = translation.getTranslatedText();
                if (content.length() > 0) {

                    Message newMessage = new Message();
                    newMessage.text = content;
                    newMessage.orignal_text = msg;
                    newMessage.idSender = StaticConfig.UID;
                    newMessage.idReceiver = roomId;
                    newMessage.timestamp = System.currentTimeMillis();
                    FirebaseDatabase.getInstance().getReference().child("message/" + roomId).push().setValue(newMessage);
                }
            }
        });

        //return msg;
    }


}

class ListMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private Consersation consersation;
    private HashMap<String, Bitmap> bitmapAvata;
    private HashMap<String, DatabaseReference> bitmapAvataDB;
    private Bitmap bitmapAvataUser;
    private CloudNaturalLanguage naturalLanguageService;
    private float score=0;
    private int mColorPositive;
    private int mColorNeutral;
    private int mColorNegative;
    private float sentiment = 0;


    public ListMessageAdapter(Context context, Consersation consersation, HashMap<String, Bitmap> bitmapAvata, Bitmap bitmapAvataUser) {
        this.context = context;
        this.consersation = consersation;
        this.bitmapAvata = bitmapAvata;
        this.bitmapAvataUser = bitmapAvataUser;
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
            setSentiments(consersation.getListMessageData().get(position).text,((ItemMessageFriendHolder) holder).txtContent);

            ((ItemMessageFriendHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).text);
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
            setSentiments(consersation.getListMessageData().get(position).orignal_text, ((ItemMessageUserHolder) holder).txtContent);
            ((ItemMessageUserHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).orignal_text);
            if (bitmapAvataUser != null) {
                ((ItemMessageUserHolder) holder).avata.setImageBitmap(bitmapAvataUser);
            }
        }
    }

    private void setSentiments(final String text, final TextView txtContent) {

            naturalLanguageService =
                    new CloudNaturalLanguage.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new AndroidJsonFactory(),
                            null
                    ).setCloudNaturalLanguageRequestInitializer(
                            new CloudNaturalLanguageRequestInitializer(StaticConfig.API_KEY)
                    ).build();

            String transcript = text;

            SharedPreferenceHelper prefHelper = SharedPreferenceHelper.getInstance(context);
            User user = prefHelper.getUserInfo();

            Document document = new Document();
            document.setType("PLAIN_TEXT");
            document.setLanguage(user.Native_Language);
            document.setContent(transcript);

            Features features = new Features();
            features.setExtractDocumentSentiment(true);

            final AnnotateTextRequest request = new AnnotateTextRequest();
            request.setDocument(document);
            request.setFeatures(features);

            AsyncTask.execute(new Runnable() {
                                  @Override
                                  public void run() {

                                      try {
                                          AnnotateTextResponse response =
                                                  naturalLanguageService.documents()
                                                          .annotateText(request).execute();

                                          sentiment = response.getDocumentSentiment().getScore();
                                          if (sentiment > 0.0) {
                                              txtContent.setBackgroundColor(mColorPositive);
                                          } else if (sentiment < 0.0) {
                                              txtContent.setBackgroundColor(mColorNegative);
                                          } else {
                                              txtContent.setBackgroundColor(mColorNeutral);
                                          }


                                          Log.e("senti", text + "= " + sentiment);

                                      } catch (IOException e) {
                                          e.printStackTrace();
                                      }

                                  }
                              }
            );



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

    public ItemMessageFriendHolder(View itemView) {
        super(itemView);
        txtContent = (TextView) itemView.findViewById(R.id.textContentFriend);
        avata = (CircleImageView) itemView.findViewById(R.id.imageView3);
    }
}
