package no.synth.skbankwrapper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;
import no.synth.skbankwrapper.activity.BaseAppWebViewActivity;
import no.synth.skbankwrapper.preferences.AppPreferences;
import no.synth.skbankwrapper.util.Logger;
import no.synth.skbankwrapper.util.OrbotHelper;
import no.synth.skbankwrapper.webview.AppWebView;

/**
 * Facebook web wrapper activity.
 */
public class AppWrapper extends BaseAppWebViewActivity {

    // Constant
    private final static String LOG_TAG = "AppWrapper";
    private final static int MENU_DRAWER_GRAVITY = GravityCompat.END;
    protected final static int DELAY_RESTORE_STATE = (60 * 1000) * 30;

    // Members
    private DrawerLayout mDrawerLayout = null;
    private RelativeLayout mWebViewContainer = null;
    private String mDomainToUse = INIT_URL_MOBILE;

    // Preferences stuff
    private SharedPreferences mSharedPreferences = null;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityCreated() {
        Logger.d(LOG_TAG, "onActivityCreated()");

        // Set the content view layout
        setContentView(R.layout.main_layout);

        // Keep a reference of the DrawerLayout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.layout_main);
        mWebViewContainer = (RelativeLayout) findViewById(R.id.webview_container);

        // Set the click listener interface for the buttons
        setOnClickListeners();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onWebViewInit(Bundle savedInstanceState) {
        Logger.d(LOG_TAG, "onWebViewInit()");

        // Load the application's preferences
        loadPreferences();

        // Get the Intent data in case we need to load a specific URL
        Intent intent = getIntent();

        // Get a subject and text and check if this is a link trying to be shared
        String sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        String sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT);

        // If we have a valid URL that was shared to us, open the sharer
        if (sharedUrl != null) {
            if (!sharedUrl.equals("")) {

                // Check if the URL being shared is a proper web URL
                if (!sharedUrl.startsWith("http://") || !sharedUrl.startsWith("https://")) {
                    // if it's not, let's see if it includes a URL in it (prefixed with a message)
                    int startUrlIndex = sharedUrl.indexOf("http:");
                    if (startUrlIndex > 0) {
                        // Seems like it's prefixed with a message, let's trim the start and get the URL only
                        sharedUrl = sharedUrl.substring(startUrlIndex);
                    }
                }

                String formattedSharedUrl = String.format(mDomainToUse + URL_PAGE_SHARE_LINKS,
                        sharedUrl, sharedSubject);
                Logger.d(LOG_TAG, "Loading the sharer page...");
                loadNewPage(Uri.parse(formattedSharedUrl).toString());
                return;
            }
        }

        // Open the proper URL in case the user clicked on a link that brought us here
        if (intent.getData() != null) {
            Logger.d(LOG_TAG, "Loading a specific predefined URL a user " +
                    "clicked on somewhere else");
            loadNewPage(intent.getData().toString());
            return;
        }

        boolean loadInitialPage = true;

        if (savedInstanceState != null) {
            long savedStateTime = savedInstanceState.getLong(KEY_SAVE_STATE_TIME, -1);
            if (savedStateTime > 0) {
                long timeDiff = System.currentTimeMillis() - savedStateTime;
                if ((mWebView != null) && (timeDiff < DELAY_RESTORE_STATE)) {
                    // Restore the state of the WebView using the saved instance state
                    Logger.d(LOG_TAG, "Restoring the WebView state");
                    restoreWebView(savedInstanceState);
                    loadInitialPage = false;
                }
            }
        }

        if (loadInitialPage) {
            // Load the URL depending on the type of device or preference
            Logger.d(LOG_TAG, "Loading the init URL");
            loadNewPage(mDomainToUse);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResumeActivity() {
        Logger.d(LOG_TAG, "onResumeActivity()");

        // This will allow us to check and see if the domain to be used changed
        String previousDomainUsed = mDomainToUse;

        // Reload the preferences in case the user changed something critical
        loadPreferences();

        // If the domain changes, reload the page with the new domain
        if (!mDomainToUse.equalsIgnoreCase(previousDomainUsed)) {
            loadNewPage(mDomainToUse);
        }
    }

    /**
     * Sets the click listener on all the buttons in the activity
     */
    private void setOnClickListeners() {
        // Create a new listener
        MenuDrawerButtonListener buttonsListener = new MenuDrawerButtonListener();

        // Set this listener to all the buttons
        findViewById(R.id.menu_drawer_right).setOnClickListener(buttonsListener);
        findViewById(R.id.menu_item_jump_to_top).setOnClickListener(buttonsListener);
        findViewById(R.id.menu_item_refresh).setOnClickListener(buttonsListener);
        findViewById(R.id.menu_item_total).setOnClickListener(buttonsListener);
        findViewById(R.id.menu_item_payment).setOnClickListener(buttonsListener);
        findViewById(R.id.menu_item_favorites).setOnClickListener(buttonsListener);
        findViewById(R.id.menu_item_accountstatement).setOnClickListener(buttonsListener);
        findViewById(R.id.menu_share_this).setOnClickListener(buttonsListener);
        findViewById(R.id.menu_preferences).setOnClickListener(buttonsListener);
        findViewById(R.id.menu_about).setOnClickListener(buttonsListener);
        findViewById(R.id.menu_kill).setOnClickListener(buttonsListener);
    }

    /**
     * Used to open the menu drawer
     */
    private void openMenuDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(MENU_DRAWER_GRAVITY);
        }
    }

    /**
     * Used to close the menu drawer
     */
    private void closeMenuDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(MENU_DRAWER_GRAVITY);
        }
    }

    /**
     * Check to see if the menu drawer is currently open
     *
     * @return {@link boolean} true if the menu drawer is open,
     *         false if closed.
     */
    private boolean isMenuDrawerOpen() {
        if (mDrawerLayout != null) {
            return mDrawerLayout.isDrawerOpen(MENU_DRAWER_GRAVITY);
        } else {
            return false;
        }
    }

    /**
     * Used to toggle the menu drawer
     */
    private void toggleMenuDrawer() {
        if (isMenuDrawerOpen()) {
            closeMenuDrawer();
        } else {
            openMenuDrawer();
        }
    }

    /**
     * Set the preferences for this activity by using the
     * {@link PreferenceManager} to load the Default shared preferences.<br />
     * Most preferences will be automatically set for the {@link AppWebView}.
     */
    private void loadPreferences() {

        if (mSharedPreferences == null) {
            // Get the default shared preferences instance
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        }

        // Get the URL to load, check-in and proxy settings
        boolean anyDomain = mSharedPreferences.getBoolean(AppPreferences.OPEN_LINKS_INSIDE, false);
        boolean allowCheckins = mSharedPreferences.getBoolean(AppPreferences.ALLOW_CHECKINS, false);
        boolean blockImages = mSharedPreferences.getBoolean(AppPreferences.BLOCK_IMAGES, false);
        boolean enableProxy = mSharedPreferences.getBoolean(AppPreferences.KEY_PROXY_ENABLED, false);
        String proxyHost = mSharedPreferences.getString(AppPreferences.KEY_PROXY_HOST, null);
        String proxyPort = mSharedPreferences.getString(AppPreferences.KEY_PROXY_PORT, null);

        // Set the flags for loading URLs, allowing geolocation and loading network images
        setAllowCheckins(allowCheckins);
        setAllowAnyDomain(anyDomain);
        setBlockImages(blockImages);

        if (enableProxy && !TextUtils.isEmpty(proxyHost) && !TextUtils.isEmpty(proxyPort)) {
            int proxyPortInt = -1;
            try {
                proxyPortInt = Integer.parseInt(proxyPort);
            } catch (Exception e) {
                e.printStackTrace();
            }
            setProxy(proxyHost, proxyPortInt);

            // If Orbot is installed and not running, request to start it
            OrbotHelper orbotHelper = new OrbotHelper(this);
            if (orbotHelper.isOrbotInstalled() && !orbotHelper.isOrbotRunning()) {
                orbotHelper.requestOrbotStart(this);
            }
        }

        // Whether the site should be loaded as the mobile or desktop version
        String mode = mSharedPreferences.getString(AppPreferences.SITE_MODE,
                AppPreferences.SITE_MODE_AUTO);

        // TODO: time to fix this mess
        // Force or detect the site mode to load
        if (mode.equalsIgnoreCase(AppPreferences.SITE_MODE_MOBILE)) {
            // Force the webview config to mobile
            setupAppWebViewConfig(true, true, false, false, false);
        } else if (mode.equalsIgnoreCase(AppPreferences.SITE_MODE_DESKTOP)) {
            // Force the webview config to desktop mode
            setupAppWebViewConfig(true, false, false, false, false);
        } else if (mode.equalsIgnoreCase(AppPreferences.SITE_MODE_ZERO)) {
            // Force the webview config to zero mode
            setupAppWebViewConfig(false, true, false, true, false);
        } else if (mode.equalsIgnoreCase(AppPreferences.SITE_MODE_BASIC)) {
            // Force the webview to load the Basic HTML Mobile site
            setupAppWebViewConfig(true, true, true, false, false);
        } else if (mode.equalsIgnoreCase(AppPreferences.SITE_MODE_ONION)) {
            // Force the webview to load Facebook via Tor (onion network)
            setupAppWebViewConfig(true, true, false, false, true);
        } else {
            // Do not force, allow us to auto-detect what mode to use
            setupAppWebViewConfig(false, true, false, false, false);
        }

        // If we haven't shown the new menu drawer to the user, auto open it
        if (!mSharedPreferences.getBoolean(AppPreferences.MENU_DRAWER_SHOWED_OPENED, false)) {
            openMenuDrawer();

            // Make sure we don't auto-open the menu ever again
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(AppPreferences.MENU_DRAWER_SHOWED_OPENED, true);
            editor.apply();
        }

    }

    /**
     * Configure this {@link AppWebView}
     * with the appropriate preferences depending on the device configuration.<br />
     * Use the 'force' flag to force the configuration to either mobile or desktop.
     *
     * @param force  {@link boolean}
     *               whether to force the configuration or not,
     *               if false the 'mobile' flag will be ignored
     * @param mobile {@link boolean}
     *               whether to use the mobile or desktop site.
     * @param appZero {@link boolean}
     *               whether or not to use Facebook Zero
     */
    // TODO: time to fix this mess
    private void setupAppWebViewConfig(boolean force, boolean mobile, boolean facebookBasic,
                                       boolean appZero, boolean appOnion) {
        if (force && !mobile) {
            // Force the desktop site to load
            mDomainToUse = INIT_URL_DESKTOP;
        } else if (appZero) {
            // If Facebook zero is set, use that
            mDomainToUse = INIT_URL_APP_ZERO;
        } else if (appOnion) {
            // If the Onion domain is set, use that
            mDomainToUse = INIT_URL_APP_ONION;
        } else {
            // Otherwise, just load the mobile site for all devices
            mDomainToUse = INIT_URL_MOBILE;
        }

        // Set the user agent depending on config
        setUserAgent(force, mobile, facebookBasic);
    }

    /**
     * Check whether this device is a phone or a tablet.
     *
     * @return {@link boolean} whether this device is a phone
     *         or a tablet
     */
    private boolean isDeviceTablet() {
        boolean isTablet;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // There were no tablet devices before Honeycomb, assume is a phone
            isTablet = false;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR2) {
            // Honeycomb only allowed tablets, always assume it's a tablet
            isTablet = true;
        } else {
            // If the device's screen width is higher than 720dp, it's a tablet,
            // otherwise, it's, at least, a phone (could be a phablet, or small tablet)
            Configuration config = getResources().getConfiguration();
            isTablet = config.smallestScreenWidthDp >= 720;
        }

        return isTablet;
    }

    /**
     * Menu drawer button listener interface
     */
    private class MenuDrawerButtonListener implements View.OnClickListener {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.menu_item_jump_to_top:
                    jumpToTop();
                    break;
                case R.id.menu_item_refresh:
                    refreshCurrentPage();
                    break;
                case R.id.menu_item_payment:
                    loadNewPage(mDomainToUse + URL_PAGE_PAYMENT);
                    break;
                case R.id.menu_item_favorites:
                    loadNewPage(mDomainToUse + URL_PAGE_FAVORITES);
                    break;
                case R.id.menu_item_total:
                    loadNewPage(mDomainToUse + URL_PAGE_TOTAL);
                    break;
                case R.id.menu_item_accountstatement:
                    loadNewPage(mDomainToUse + URL_PAGE_ACCOUNTSTATEMENT);
                    break;
                case R.id.menu_share_this:
                    shareCurrentPage();
                    break;
                case R.id.menu_preferences:
                    startActivity(new Intent(AppWrapper.this, AppPreferences.class));
                    break;
                case R.id.menu_about:
                    showAboutAlert();
                    break;
                case R.id.menu_kill:
                    mWebViewContainer.removeView(mWebView);
                    destroyWebView();
                    finish();
                    break;
            }
            closeMenuDrawer();
        }
    }

    /**
     * Show an alert dialog with the information about the application.
     */
    private void showAboutAlert() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.menu_about));
        alertDialog.setMessage(getString(R.string.txt_about));
        alertDialog.setIcon(R.drawable.ic_launcher);
        alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL,
                getString(R.string.lbl_dialog_close),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Don't do anything, simply close the dialog
                    }
                });
        alertDialog.show();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode){
        case KeyEvent.KEYCODE_MENU:
            toggleMenuDrawer();
            return true;
        case KeyEvent.KEYCODE_BACK:
            // If the back button is pressed while the drawer
            // is open try to close it
            if (isMenuDrawerOpen()) {
                closeMenuDrawer();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

}
