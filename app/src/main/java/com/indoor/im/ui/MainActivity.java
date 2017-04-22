package com.indoor.im.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.gitonway.lee.niftynotification.lib.Effects;
import com.indoor.im.CustomApplcation;
import com.indoor.im.MyMessageReceiver;
import com.indoor.im.R;
import com.indoor.im.ui.fragment.ContactFragment;
import com.indoor.im.ui.fragment.NewsmediaFragment;
import com.indoor.im.ui.fragment.Online_shoppingFragment;
import com.indoor.im.ui.fragment.RecentFragment;
import com.indoor.im.ui.fragment.SettingsFragment;

import cn.bmob.im.BmobChatManager;
import cn.bmob.im.BmobNotifyManager;
import cn.bmob.im.bean.BmobInvitation;
import cn.bmob.im.bean.BmobMsg;
import cn.bmob.im.config.BmobConfig;
import cn.bmob.im.db.BmobDB;
import cn.bmob.im.inteface.EventListener;

/**
 * 登陆
 *
 * @author smile
 * @ClassName: MainActivity
 * @Description: TODO
 * @date 2014-5-29 下午2:45:35
 */
public class MainActivity extends ActivityBase implements EventListener {

    private static long firstTime;
    ImageView iv_recent_tips, iv_contact_tips;//消息提示
    NewBroadcastReceiver newReceiver;
    TagBroadcastReceiver userReceiver;
    private Button[] mTabs;
    private ContactFragment contactFragment;
    private RecentFragment recentFragment;
    private NewsmediaFragment newsmediaFragment;
    private Online_shoppingFragment online_shoppingFragment;
    private SettingsFragment settingFragment;
    private Fragment[] fragments;
    private int index;
    private int currentTabIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //开启定时检测服务（单位为秒）-在这里检测后台是否还有未读的消息，有的话就取出来
        //如果你觉得检测服务比较耗流量和电量，你也可以去掉这句话-同时还有onDestory方法里面的stopPollService方法
        //BmobChat.getInstance(this).startPollService(30);
        //开启广播接收器
        initNewMessageBroadCast();
        initTagMessageBroadCast();
        initView();
        initTab();
    }

    private void initView() {
        mTabs = new Button[5];
        mTabs[0] = (Button) findViewById(R.id.btn_message);
        mTabs[1] = (Button) findViewById(R.id.btn_contract);
        mTabs[2] = (Button) findViewById(R.id.btn_newsmedia);
        mTabs[3] = (Button) findViewById(R.id.btn_shopping);
        mTabs[4] = (Button) findViewById(R.id.btn_set);
        iv_recent_tips = (ImageView) findViewById(R.id.iv_recent_tips);
        iv_contact_tips = (ImageView) findViewById(R.id.iv_contact_tips);
        //把第一个tab设为选中状态
        mTabs[0].setSelected(true);
    }

    private void initTab() {
        recentFragment = new RecentFragment();
        contactFragment = new ContactFragment();
        newsmediaFragment = new NewsmediaFragment();
        online_shoppingFragment = new Online_shoppingFragment();
        settingFragment = new SettingsFragment();
        fragments = new Fragment[]{recentFragment, contactFragment, newsmediaFragment, online_shoppingFragment, settingFragment};
        // 添加显示第一个fragment
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, recentFragment).
                add(R.id.fragment_container, contactFragment).hide(contactFragment).show(recentFragment).commit();
    }

    /**
     * button点击事件
     *
     * @param view
     */
    public void onTabSelect(View view) {
        switch (view.getId()) {
            case R.id.btn_message:
                index = 0;
                break;
            case R.id.btn_contract:
                index = 1;
                break;
            case R.id.btn_newsmedia:
                index = 2;
                break;
            case R.id.btn_shopping:
                index = 3;
                break;
            case R.id.btn_set:
                index = 4;
                break;
        }
        if (currentTabIndex != index) {
            FragmentTransaction trx = getSupportFragmentManager().beginTransaction();
            trx.hide(fragments[currentTabIndex]);
            if (!fragments[index].isAdded()) {
                trx.add(R.id.fragment_container, fragments[index]);
            }
            trx.show(fragments[index]).commit();
        }
        mTabs[currentTabIndex].setSelected(false);
        //把当前tab设为选中状态
        mTabs[index].setSelected(true);
        currentTabIndex = index;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //小圆点提示
        if (BmobDB.create(this).hasUnReadMsg()) {
            iv_recent_tips.setVisibility(View.VISIBLE);
        } else {
            iv_recent_tips.setVisibility(View.GONE);
        }
        if (BmobDB.create(this).hasNewInvite()) {
            iv_contact_tips.setVisibility(View.VISIBLE);
        } else {
            iv_contact_tips.setVisibility(View.GONE);
        }
        MyMessageReceiver.ehList.add(this);// 监听推送的消息
        //清空
        MyMessageReceiver.mNewNum = 0;
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyMessageReceiver.ehList.remove(this);// 取消监听推送的消息
    }

    @Override
    public void onMessage(BmobMsg message) {
        refreshNewMsg(message);
    }

    /**
     * 刷新界面
     *
     * @param @param message
     * @return void
     * @throws
     * @Title: refreshNewMsg
     * @Description: TODO
     */
    private void refreshNewMsg(BmobMsg message) {
        // 声音提示
        boolean isAllow = CustomApplcation.getInstance().getSpUtil().isAllowVoice();
        if (isAllow) {
            CustomApplcation.getInstance().getMediaPlayer().start();
        }
        iv_recent_tips.setVisibility(View.VISIBLE);
        //也要存储起来
        if (message != null) {
            BmobChatManager.getInstance(MainActivity.this).saveReceiveMessage(true, message);
        }
        if (currentTabIndex == 0) {
            //当前页面如果为会话页面，刷新此页面
            if (recentFragment != null) {
                recentFragment.refresh();
            }
        }
    }

    private void initNewMessageBroadCast() {
        // 注册接收消息广播
        newReceiver = new NewBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(BmobConfig.BROADCAST_NEW_MESSAGE);
        //优先级要低于ChatActivity
        intentFilter.setPriority(3);
        registerReceiver(newReceiver, intentFilter);
    }

    private void initTagMessageBroadCast() {
        // 注册接收消息广播
        userReceiver = new TagBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(BmobConfig.BROADCAST_ADD_USER_MESSAGE);
        //优先级要低于ChatActivity
        intentFilter.setPriority(3);
        registerReceiver(userReceiver, intentFilter);
    }

    @Override
    public void onNetChange(boolean isNetConnected) {
        if (isNetConnected) {
            showTag("当前网络不可用,请检查您的网络!", Effects.standard, R.id.main_bottom);
        }
    }

    @Override
    public void onAddUser(BmobInvitation message) {
        refreshInvite(message);
    }

    /**
     * 刷新好友请求
     *
     * @param @param message
     * @return void
     * @throws
     * @Title: notifyAddUser
     * @Description: TODO
     */
    private void refreshInvite(BmobInvitation message) {
        boolean isAllow = CustomApplcation.getInstance().getSpUtil().isAllowVoice();
        if (isAllow) {
            CustomApplcation.getInstance().getMediaPlayer().start();
        }
        iv_contact_tips.setVisibility(View.VISIBLE);
        if (currentTabIndex == 1) {
            if (contactFragment != null) {
                contactFragment.refresh();
            }
        } else {
            //同时提醒通知
            String tickerText = message.getFromname() + "请求添加好友";
            boolean isAllowVibrate = CustomApplcation.getInstance().getSpUtil().isAllowVibrate();
            BmobNotifyManager.getInstance(this).showNotify(isAllow, isAllowVibrate, R.drawable.ic_launcher,
                    tickerText, message.getFromname(), tickerText.toString(), NewFriendActivity.class);
        }
    }

    @Override
    public void onOffline() {
        showOfflineDialog(this);
    }

    @Override
    public void onReaded(String conversionId, String msgTime) {
    }

    /**
     * 连续按两次返回键就退出
     */
    @Override
    public void onBackPressed() {
        if (firstTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            showTag("再按一次退出程序", Effects.standard, R.id.main_bottom);
        }
        firstTime = System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(newReceiver);
        } catch (Exception e) {
        }
        try {
            unregisterReceiver(userReceiver);
        } catch (Exception e) {
        }
        //取消定时检测服务
//		BmobChat.getInstance(this).stopPollService();
    }

    /**
     * 新消息广播接收者
     */
    private class NewBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //刷新界面
            refreshNewMsg(null);
            // 记得把广播给终结掉
            abortBroadcast();
        }
    }

    /**
     * 标签消息广播接收者
     */
    private class TagBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            BmobInvitation message = (BmobInvitation) intent.getSerializableExtra("invite");
            refreshInvite(message);
            // 记得把广播给终结掉
            abortBroadcast();
        }
    }

}