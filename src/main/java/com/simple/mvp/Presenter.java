package com.simple.mvp;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import java.lang.ref.WeakReference;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

/**
 * Mvp Presenter 抽象类. 通过 弱引用持有 Context 和 View对象, 避免产生内存泄露。
 *
 * <p>
 * 当 Context (通常是指Activity)被销毁时如果客户端程序
 * 再调用Context, 那么直接返回 Application 的Context. 因此如果用户需要调用与Activity相关的UI操作(例如弹出Dialog)时,
 * 应该先调用 {@link #isActivityAlive()} 来判断Activity是否还存活.
 * </p>
 *
 * <p>
 * 当 View 对象销毁时如果用户再调用 View对象, 那么则会
 * 通过动态代理创建一个View对象 {@link #mNullView}, 这样保证 view对象不会为空.
 * </p>
 *
 * Created by mrsimple on 23/12/16.
 */
public abstract class Presenter<T extends MvpView> {
    /**
     * application context
     */
    private static Context sAppContext;
    /**
     * context weak reference
     */
    private WeakReference<Context> mContextRef;
    /**
     * mvp view weak reference
     */
    private WeakReference<T> mViewRef;
    /**
     * mvp view class
     */
    private Class mMvpViewClass = null;
    /**
     * Mvp View created by dynamic Proxy
     */
    private T mNullView;
    /**
     * init application context with reflection.
     */
    static {
        try {
            // 先通过 ActivityThread 来获取 Application Context
            Application application = (Application) Class.forName("android.app.ActivityThread").getMethod
                    ("currentApplication").invoke(null, (Object[]) null);
            if (application != null) {
                sAppContext = application;
            }
            if (sAppContext == null) {
                // 第二种方式初始化
                application = (Application) Class.forName("android.app.AppGlobals").getMethod("getInitialApplication")
                        .invoke(null, (Object[]) null);
                if (application != null) {
                    sAppContext = application;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public Presenter() {
    }


    public Presenter(Context context, T view) {
        attach(context, view);
    }

    /**
     * attach context & mvp view
     * @param context
     * @param view
     */
    public void attach(Context context, T view) {
        mContextRef = new WeakReference<>(context);
        mViewRef = new WeakReference<>(view);
        if (sAppContext == null && context != null) {
            sAppContext = context.getApplicationContext();
        }
    }

    /**
     * release resource
     */
    public void detach() {

    }

    /**
     * UI展示相关的操作需要判断一下 Activity 是否已经 finish.
     * <p>
     * todo : 只有当 isActivityAlive 返回true时才可以执行与Activity相关的操作,
     * 比如 弹出Dialog、Window、跳转Activity等操作.
     *
     * @return
     */
    protected boolean isActivityAlive() {
        return !isActivityFinishing();
    }


    /**
     * 返回 Context. 如果 Activity被销毁, 那么返回应用的Context
     *
     * @return
     */
    protected Context getContext() {
        Context context = mContextRef != null ? mContextRef.get() : null;
        if (context == null || isActivityFinishing()) {
            context = sAppContext;
        }
        return context;
    }

    /**
     * activity 是否是finishing状态
     *
     * @return
     */
    private boolean isActivityFinishing() {
        if (mContextRef == null) {
            return true;
        }
        Context context = mContextRef.get();
        if (context instanceof Activity) {
            Activity hostActivity = (Activity) context;
            return hostActivity.isFinishing();
        }
        return false;
    }


    protected T getMvpView() {
        T view = mViewRef != null ? mViewRef.get() : null;
        if (view == null) {
            // create null mvp view
            if (mNullView == null) {
                mNullView = (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{getMvpViewClass()},
                        new MvpViewInvocationHandler());
            }
            view = mNullView;
        }
        return view;
    }


    private Class getMvpViewClass() {
        if (mMvpViewClass == null) {
            Type genType = getClass().getGenericSuperclass();
            Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
            mMvpViewClass = (Class) params[0];
        }
        return mMvpViewClass;
    }


    protected String getString(int rid) {
        return getContext().getString(rid);
    }
}
