package cz.slvikblue.radio;

import android.app.*;
import android.content.*;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.*;
import android.widget.Toast;
import java.io.IOException;

public class RadioService extends Service {
    static final String PLAY="cz.slvikblue.radio.PLAY", STOP="cz.slvikblue.radio.STOP", TOGGLE="cz.slvikblue.radio.TOGGLE";
    private static final int NOTIFICATION_ID=71;
    private MediaPlayer player;
    private String station="Internet Radio";
    private String streamUrl;
    private PowerManager.WakeLock wakeLock;

    @Override public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent i,int flags,int id) {
        if(i==null) return START_NOT_STICKY;
        String a=i.getAction();
        if(STOP.equals(a)){ stopPlayback(); stopSelf(); return START_NOT_STICKY; }
        if(TOGGLE.equals(a)){ if(player!=null && player.isPlaying()){player.pause(); releaseWake();} else if(player!=null){player.start(); acquireWake();} updateNotification(); return START_STICKY; }
        if(PLAY.equals(a)){
            streamUrl=i.getStringExtra("url"); station=i.getStringExtra("name");
            startForeground(NOTIFICATION_ID, notification("Připojuji…"));
            startPlayback();
        }
        return START_STICKY;
    }
    private void startPlayback(){
        stopPlayerOnly();
        player=new MediaPlayer();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setOnPreparedListener(mp->{mp.start(); acquireWake(); updateNotification();});
        player.setOnErrorListener((mp,what,extra)->{ updateNotification("Nepodařilo se přehrát stanici"); stopPlayerOnly(); return true;});
        player.setOnInfoListener((mp,what,extra)->false);
        try { player.setDataSource(streamUrl); player.prepareAsync(); }
        catch(IOException e){ updateNotification("Chyba streamu"); stopPlayerOnly(); }
    }
    private void stopPlayerOnly(){ if(player!=null){try{player.reset();player.release();}catch(Exception ignored){} player=null;} releaseWake(); }
    private void stopPlayback(){ stopPlayerOnly(); stopForeground(true); }
    private void acquireWake(){ try {PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE); if(wakeLock==null) wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"internetRadio:audio"); if(!wakeLock.isHeld()) wakeLock.acquire();}catch(Exception ignored){} }
    private void releaseWake(){try{if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();}catch(Exception ignored){}}
    private PendingIntent action(String a,int req){Intent i=new Intent(this,RadioService.class).setAction(a);return PendingIntent.getService(this,req,i,PendingIntent.FLAG_UPDATE_CURRENT);}
    private Notification notification(String text){
        if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel("radio_playback","Přehrávání rádia",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);}
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"radio_playback"):new Notification.Builder(this);
        boolean playing=player!=null&&player.isPlaying();
        b.setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(station==null?"Internet Radio":station).setContentText(text)
         .setOngoing(playing||streamUrl!=null).addAction(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,playing?"Pauza":"Pokračovat",action(TOGGLE,2))
         .addAction(android.R.drawable.ic_media_stop,"Zastavit",action(STOP,3));
        return b.build();
    }
    private void updateNotification(){updateNotification(player!=null&&player.isPlaying()?"Právě hraje":"Pozastaveno");}
    private void updateNotification(String text){((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,notification(text));}
    @Override public void onDestroy(){stopPlayback();super.onDestroy();}
}
