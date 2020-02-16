package edu.cuc.ccc.helpers;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.util.Log;

import java.io.IOException;

import edu.cuc.ccc.Device;
import edu.cuc.ccc.R;
import edu.cuc.ccc.backends.BackendService;

public class NFCHelper {
    private static String TAG = NFCHelper.class.getSimpleName();

    private Context mContext;

    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;

    private Tag mTag;

    private NFCTagEventListener mListener;

    private Device targetDevice;
//    private boolean enable = false;

    public NFCHelper(Context context) {
        this.mContext = context;
        mAdapter = NfcAdapter.getDefaultAdapter(mContext);
        mPendingIntent = PendingIntent.getActivity(mContext, 0,
                new Intent(mContext, mContext.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        ndef.addCategory(Intent.CATEGORY_DEFAULT);
        mFilters = new IntentFilter[]{ndef};
        mTechLists = new String[][]{};
    }

    public void setupForegroundDispatch() {
//        if (!enable) return;
        if (mAdapter == null) return;
        mAdapter.enableForegroundDispatch((Activity) mContext, mPendingIntent, mFilters, mTechLists);
        Log.i("Foreground dispatch", "setupForegroundDispatch");
    }

    public void undoForegroundDispatch() {
//        if (!enable) return;
        if (mAdapter == null) return;
        mAdapter.disableForegroundDispatch((Activity) mContext);
        Log.i("Foreground dispatch", "undoForegroundDispatch");
    }

    // 当intent为NFC_TAG_DISCOVERED才会进入这个函数
    public void resolveNFCIntent(Intent intent) {
        Log.i("Foreground dispatch", "Discovered tag with intent: " + intent);
        mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        // 有写入数据时才写入内容嘛
        if (targetDevice == null) {
//            mListener.onNFCTagWriteError(R.string.tag_unknown_device);
            targetDevice = BackendService.getInstance().getDeviceManager().getPairedDevice();
        }
        if (targetDevice != null) {
//            if (enable)
            writeTag();
        }
        // TODO:判断标签是否已经写过同类型的数据？
    }

    // TODO:检测NFC功能是否存在，并且判断是否已经开启，并显示提示消息
    public boolean isSupportNFC() {
        return (mAdapter != null);
    }

    public boolean isEnableNFC() {
        return mAdapter.isEnabled();
    }

    public void setNFCTagWriteListener(NFCTagEventListener listener) {
        mListener = listener;
    }

//    public void refreshWriteContent() {
//        targetDevice = BackendService.getInstance().getDeviceManager().getPairedDevice();
//    }

    public interface NFCTagEventListener {
        void onNFCTagWriteCompleted();

        void onNFCTagWriteError(int strId);
    }

    private NdefMessage createNdefMessage() {
        // 参考资料：https://blog.csdn.net/coslay/article/details/24743791
        // 经过多次尝试，终于实现了NFC触发后启动特定Activity
        // 要点是：第一条记录在清单文件中有所表示，最后一条记录是AAR
        // NdefMessage msgs = new NdefMessage(new NdefRecord[]{NdefRecord.createUri("ccc://devname")});
        // NdefMessage msgs = new NdefMessage(new NdefRecord[]{NdefRecord.createExternal("ccc.local", "test", new byte[0])});
        return new NdefMessage(new NdefRecord[]{
                NdefRecord.createExternal("ccc.local", "ccctag", new byte[0]),
                // TODO:sth about device
                NdefRecord.createApplicationRecord(mContext.getPackageName())});
    }

    private void writeTag() {
        // 首先创建NdefMessage，然后判断可行性，最后写入
        NdefMessage msgs = createNdefMessage();
        int size = msgs.toByteArray().length;
        Ndef ndef = Ndef.get(mTag);
        if (ndef == null) {
            NdefFormatable format = NdefFormatable.get(mTag);
            if (format == null) {
                Log.i(TAG, "writeTag: 不支持的卡类型。");
                mListener.onNFCTagWriteError(R.string.tag_nosupport);
            } else {
                try {
                    format.connect();
                    format.format(msgs);
                } catch (FormatException | IOException e) {
                    Log.e(TAG, "writeTag: 写卡错误。");
                    mListener.onNFCTagWriteError(R.string.tag_write_err);
                }
            }
        } else {
            if (!ndef.isWritable()) {
                Log.e(TAG, "writeTag: 不可写的卡。");
                mListener.onNFCTagWriteError(R.string.tag_write_err);
            } else if (ndef.getMaxSize() < size) {
                Log.e(TAG, "writeTag: 数据量过大。");
                mListener.onNFCTagWriteError(R.string.tag_write_err);
            } else {
                try {
                    ndef.connect();
                    ndef.writeNdefMessage(msgs);
                } catch (FormatException | IOException e) {
                    Log.e(TAG, "writeTag: 写卡错误。");
                    mListener.onNFCTagWriteError(R.string.tag_write_err);
                }
            }
        }
        Log.i(TAG, "writeTag: 写卡成功。");
        mListener.onNFCTagWriteCompleted();
    }

    // 设置需要写入的设备信息
    public void setWriteContent(Device device) {
        this.targetDevice = device;
    }
//
//    public void setEnable(boolean en) {
//        this.enable = en;
//    }
}
