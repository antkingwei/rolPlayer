package com.rolmex.android.rolplayer.task;

import com.rolmex.android.rolplayer.model.CommendResult;

import android.os.AsyncTask;

public abstract class CommendTask extends AsyncTask<Void,Void,CommendResult>{
    
    public interface CTaskCallback{
        void onTaskComplete(CommendTask task,CommendResult result);
    }
    private CTaskCallback callback;
    public CommendTask(CTaskCallback callback){
        this.callback = callback;
    }
    @Override
    protected void onPostExecute(CommendResult result){
        if(callback !=null){
            callback.onTaskComplete(this, result);
        }
    }

}
