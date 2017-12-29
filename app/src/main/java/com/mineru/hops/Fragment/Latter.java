package com.mineru.hops.Fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mineru.hops.Grid_Activity;
import com.mineru.hops.UserManage.Model.ImageDTO;
import com.mineru.hops.Function.MakeCard.MakeCard1;
import com.mineru.hops.MessageBoard;
import com.mineru.hops.R;
import com.mineru.hops.UserManage.Model.MessageBoard_Model;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Created by Mineru on 2017-09-22.
 */

public class Latter extends Fragment {
    private static final String TAG ="LatterFragment";
    private FloatingActionMenu fam;
    private com.github.clans.fab.FloatingActionButton fabWrite;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.latter_fragment_layout,container,false);

        RecyclerView recyclerView  = (RecyclerView) view.findViewById(R.id.chatfragment_recyclerview);
        recyclerView.setAdapter(new ChatRecyclerViewAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));

        fabWrite = (com.github.clans.fab.FloatingActionButton) view.findViewById(R.id.fab_write);
        fam = (FloatingActionMenu) view.findViewById(R.id.fab_plus_latter);

        fabWrite.setOnClickListener(onButtonClick());
        fam.setClosedOnTouchOutside(true);

        fam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fam.isOpened()) {
                    fam.close(true);
                }
            }
        });

        return view;
    }

    private View.OnClickListener onButtonClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view == fabWrite) {
                    Intent intent = new Intent(getActivity(), Grid_Activity.class);
                    startActivity(intent);
                }
                fam.close(true);
            }
        };
    }

    class ChatRecyclerViewAdapter extends  RecyclerView.Adapter<RecyclerView.ViewHolder>{

        private List<MessageBoard_Model> messaageboard = new ArrayList<>();
        private String uid;
        private ArrayList<String> destinationUsers = new ArrayList<>();

        public ChatRecyclerViewAdapter() {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseDatabase.getInstance().getReference().child("MessageBoards").orderByChild("users/"+uid).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    messaageboard.clear();
                    for (DataSnapshot item :dataSnapshot.getChildren()){
                        messaageboard.add(item.getValue(MessageBoard_Model.class));
                    }
                    notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });



        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_latter,parent,false);


            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

            final CustomViewHolder customViewHolder = (CustomViewHolder)holder;
            String destinationUid = null;

            for(String user: messaageboard.get(position).users.keySet()){
                if(!user.equals(uid)){
                    destinationUid = user;
                    destinationUsers.add(destinationUid);
                }
            }
            FirebaseDatabase.getInstance().getReference().child("Users").child(destinationUid+"/Card").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ImageDTO imageDTO = dataSnapshot.getValue(ImageDTO.class);
                    Glide.with(customViewHolder.itemView.getContext())
                            .load(imageDTO.imageUrl)
                            .apply(new RequestOptions().circleCrop())
                            .into(customViewHolder.imageView);

                    customViewHolder.textView_title.setText(imageDTO.inputName);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            Map<String,MessageBoard_Model.Comment> commentMap = new TreeMap<>(Collections.reverseOrder());
            commentMap.putAll(messaageboard.get(position).comments);
            String lastMessageKey = (String) commentMap.keySet().toArray()[0];
            customViewHolder.textView_last_message.setText(messaageboard.get(position).comments.get(lastMessageKey).message);

            customViewHolder.imageView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view){
                    Intent intent = new Intent(view.getContext(), MessageBoard.class);
                    intent.putExtra("Uid",destinationUsers.get(position));

                    ActivityOptions activityOptions = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        activityOptions = ActivityOptions.makeCustomAnimation(view.getContext(), R.anim.fromright,R.anim.toleft);
                        startActivity(intent,activityOptions.toBundle());
                    }
                }
            });
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            long unixTime = (long) messaageboard.get(position).comments.get(lastMessageKey).timestamp;
            Date date = new Date(unixTime);
            customViewHolder.textView_timestamp.setText(simpleDateFormat.format(date));
        }

        @Override
        public int getItemCount() {
            return messaageboard.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView_title;
            public TextView textView_last_message;
            public TextView textView_timestamp;

            public CustomViewHolder(View view) {
                super(view);

                imageView = (ImageView) view.findViewById(R.id.chatitem_imageview);
                textView_last_message = (TextView) view.findViewById(R.id.chatitem_textview_lastMessage);
                textView_title = (TextView) view.findViewById(R.id.chatitem_textview_title);
                textView_timestamp = (TextView) view.findViewById(R.id.chatitem_textview_timestamp);

            }
        }
    }
}