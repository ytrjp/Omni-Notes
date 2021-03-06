/*******************************************************************************
 * Copyright 2014 Federico Iosue (federico.iosue@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use mActivity file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package it.feio.android.omninotes;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;
import it.feio.android.checklistview.ChecklistManager;
import it.feio.android.checklistview.exceptions.ViewNotSupportedException;
import it.feio.android.checklistview.models.CheckListView;
import it.feio.android.omninotes.async.AttachmentTask;
import it.feio.android.omninotes.async.DeleteNoteTask;
import it.feio.android.omninotes.async.SaveNoteTask;
import it.feio.android.omninotes.db.DbHelper;
import it.feio.android.omninotes.models.Attachment;
import it.feio.android.omninotes.models.ImageAndTextItem;
import it.feio.android.omninotes.models.Note;
import it.feio.android.omninotes.models.ONStyle;
import it.feio.android.omninotes.models.PasswordValidator;
import it.feio.android.omninotes.models.Tag;
import it.feio.android.omninotes.models.adapters.AttachmentAdapter;
import it.feio.android.omninotes.models.adapters.ImageAndTextAdapter;
import it.feio.android.omninotes.models.adapters.NavDrawerTagAdapter;
import it.feio.android.omninotes.models.listeners.OnAttachingFileListener;
import it.feio.android.omninotes.models.listeners.OnReminderPickedListener;
import it.feio.android.omninotes.models.listeners.OnSketchSavedListener;
import it.feio.android.omninotes.models.views.ExpandableHeightGridView;
import it.feio.android.omninotes.utils.AlphaManager;
import it.feio.android.omninotes.utils.AppTourHelper;
import it.feio.android.omninotes.utils.Constants;
import it.feio.android.omninotes.utils.Display;
import it.feio.android.omninotes.utils.FileHelper;
import it.feio.android.omninotes.utils.Fonts;
import it.feio.android.omninotes.utils.IntentChecker;
import it.feio.android.omninotes.utils.KeyboardUtils;
import it.feio.android.omninotes.utils.StorageManager;
import it.feio.android.omninotes.utils.date.DateHelper;
import it.feio.android.omninotes.utils.date.ReminderPickers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.Toast;

import com.espian.showcaseview.ShowcaseView;
import com.espian.showcaseview.ShowcaseViews.OnShowcaseAcknowledged;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.neopixl.pixlui.components.edittext.EditText;
import com.neopixl.pixlui.components.textview.TextView;
import com.neopixl.pixlui.links.TextLinkClickListener;

import de.keyboardsurfer.android.widget.crouton.Crouton;

/**
 * An activity representing a single Item detail screen. mActivity activity is only
 * used on handset devices. On tablet-size devices, item details are presented
 * side-by-side with a list of items in a {@link ItemListActivity}.
 * <p>
 * mActivity activity is mostly just a 'shell' activity containing nothing more than
 * a {@link ItemDetailFragment}.
 */
public class DetailFragment extends Fragment implements
		OnReminderPickedListener, TextLinkClickListener, OnTouchListener,
		OnGlobalLayoutListener, OnSketchSavedListener, OnAttachingFileListener {

	private static final int TAKE_PHOTO = 1;
	private static final int GALLERY = 2;
//	private static final int RECORDING = 3;
	private static final int TAKE_VIDEO = 4;
	private static final int SET_PASSWORD = 5;
	private static final int SKETCH = 6;
	private static final int TAG = 7;
	private static final int DETAIL = 8;
	private static final int FILES = 9;

	private MainActivity mActivity;
	private LinearLayout reminder_layout;
	private TextView datetime;	
	private Uri attachmentUri;
	private AttachmentAdapter mAttachmentAdapter;
	private ExpandableHeightGridView mGridView;
	private PopupWindow attachmentDialog;
	private EditText title, content;
	private TextView locationTextView;

	private Note note;
	private Note noteTmp;

	// Reminder
	int reminderYear, reminderMonth, reminderDay;
	private String alarmDate = "", alarmTime = "";
	private String dateTimeText = "";
	public OnDateSetListener onDateSetListener;
	public OnTimeSetListener onTimeSetListener;

	// Audio recording
	private static String recordName;
	MediaRecorder mRecorder = null;
	private MediaPlayer mPlayer = null;
	private boolean isRecording = false;
	private View isPlayingView = null;
	
	private Bitmap recordingBitmap;

	// Toggle checklist view
	View toggleChecklistView;
	private ChecklistManager mChecklistManager;
	
	// Result intent
	Intent resultIntent;
	
	// Flag to check if after editing it will return to ListActivity or not
	// and in the last case a Toast will be shown instead than Crouton
	boolean afterSavedReturnsToList = true;
	private boolean swiping;
	private ViewGroup root;
	private int startSwipeX;
	private SharedPreferences prefs;
	private DbHelper db;
	private boolean onCreateOptionsMenuAlreadyCalled = false;
	private View timestampsView;
	private View keyboardPlaceholder;
	private View titleCardView;
	private boolean orientationChanged;
	private long audioRecordingTimeStart;
	private long audioRecordingTime;
	private boolean showKeyboard;
	private DetailFragment mFragment;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mActivity = (MainActivity) getActivity();
		mFragment = this;			
		prefs = mActivity.prefs;
		db = mActivity.db;
	}
	
	
	@Override
	public void onStart() {

		// GA tracking
		mActivity.gaTracker.set(Fields.SCREEN_NAME, getClass().getName());
		mActivity.gaTracker.send(MapBuilder.createAppView().build());
		
		super.onStart();
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);		
        return inflater.inflate(R.layout.fragment_detail, container, false);
	}
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		// Show the Up button in the action bar.
		if (mActivity.getSupportActionBar() != null) {
			mActivity.getSupportActionBar().setDisplayShowTitleEnabled(false);
			mActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		// Disables navigation drawer indicator (it must be only shown in ListFragment)
		if (mActivity.getDrawerToggle() != null) {
			mActivity.getDrawerToggle().setDrawerIndicatorEnabled(false);
		}
		
		// Force the navigation drawer to stay closed
		if (mActivity.getDrawerLayout() != null) {
			mActivity.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		}	
		
		resultIntent = new Intent();
		
		// Restored temp note after orientation change
		if (savedInstanceState != null) {
			noteTmp = savedInstanceState.getParcelable("note");
			attachmentUri = savedInstanceState.getParcelable("attachmentUri");
			orientationChanged = savedInstanceState.getBoolean("orientationChanged");
		}
		
		
		init();
		
		setHasOptionsMenu(true);
		setRetainInstance(false);
	}
	
	
	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {		
		noteTmp.setTitle(getNoteTitle());
		noteTmp.setContent(getNoteContent()); 
		outState.putParcelable("note", noteTmp);
		outState.putParcelable("attachmentUri", attachmentUri);
		outState.putBoolean("orientationChanged", orientationChanged);
		super.onSaveInstanceState(outState);		
	}
	
	
	
	@Override
	public void onPause() {
		super.onPause();
		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}
		restoreLayouts();
	}
	
	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (getResources().getConfiguration().orientation != newConfig.orientation) {
			orientationChanged = true;
		}
	}
	
	
	
	private void init() {
		
		// Handling of Intent actions		
		handleIntents();
		
		if (note == null) {
//			note = (Note) mActivity.getIntent().getParcelableExtra(Constants.INTENT_NOTE);	
			note = (Note) getArguments().getParcelable(Constants.INTENT_NOTE);
		}
		if (noteTmp == null) {
			if (note == null) note = new Note();
			noteTmp = new Note(note);
		}
					
		if (noteTmp != null && noteTmp.isLocked() && !noteTmp.isPasswordChecked()) {
			checkNoteLock(noteTmp);
			return;
		}	

		// Note initialization
		initNote();
		
		initViews();
	    
	    if (showKeyboard) {
//	    	// Delayed keyboard appearance
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
//					InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
//			        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
			    	KeyboardUtils.showKeyboard(content);
				}
			}, 700);
	    }
	}

	
	/**
	 * Checks note lock and password before showing note content
	 * @param note
	 */
	private void checkNoteLock(Note note) {
		// If note is locked security password will be requested
		if (noteTmp.isLocked() && prefs.getString(Constants.PREF_PASSWORD, null) != null) {
			mActivity.requestPassword(new PasswordValidator() {					
				@Override
				public void onPasswordValidated(boolean result) {
					if (result) {
						noteTmp.setPasswordChecked(true);
						init();
					} else {
						goHome();
					}
				}
			});
		} else {
			noteTmp.setPasswordChecked(true);
			init();
		}		
	}
	
	

	private void handleIntents() {
		Intent i = mActivity.getIntent();
		
		if (Constants.ACTION_MERGE.equals(i.getAction())) {
			note = new Note();
			noteTmp = (Note) getArguments().getParcelable(Constants.INTENT_NOTE);
			i.setAction(null);
		}
		
		// Action called from home shortcut
		if (Constants.ACTION_SHORTCUT.equals(i.getAction())) {
			afterSavedReturnsToList = false;
			DbHelper db = new DbHelper(mActivity);
			note = db.getNote(i.getIntExtra(Constants.INTENT_KEY, 0));
			noteTmp = new Note(note);
			// Checks if the note pointed from the shortcut has been deleted
			if (note == null || noteTmp == null) {	
				mActivity.showToast(getText(R.string.shortcut_note_deleted), Toast.LENGTH_LONG);
				mActivity.finish();
			}
			i.setAction(null);
		}
		
		// Check if is launched from a widget
		if (Constants.ACTION_WIDGET.equals(i.getAction())
			|| Constants.ACTION_WIDGET_TAKE_PHOTO.equals(i.getAction()) ) {

			afterSavedReturnsToList = false;
			
			//  with tags to set tag
			if (i.hasExtra(Constants.INTENT_WIDGET)) {
				String widgetId = i.getExtras().get(Constants.INTENT_WIDGET).toString();
				if (widgetId != null) {
					String sqlCondition = prefs.getString(Constants.PREF_WIDGET_PREFIX + widgetId, "");
					String pattern = DbHelper.KEY_TAG + " = ";
					if (sqlCondition.lastIndexOf(pattern) != -1) {
						String tagId = sqlCondition.substring(sqlCondition.lastIndexOf(pattern) + pattern.length()).trim();		
						Tag tag;
						try {
							tag = db.getTag(Integer.parseInt(tagId));
							noteTmp = new Note();
							noteTmp.setTag(tag);
						} catch (NumberFormatException e) {}			
					}
				}
			}
			
			
			// Sub-action is to take a photo
			if (Constants.ACTION_WIDGET_TAKE_PHOTO.equals(i.getAction())) {
				takePhoto();
			}
			
			i.setAction(null);			
		}
		
		
		/**
		 * Handles third party apps requests of sharing
		 */
		if ( ( Intent.ACTION_SEND.equals(i.getAction()) 
				|| Intent.ACTION_SEND_MULTIPLE.equals(i.getAction()) 
				|| Constants.INTENT_GOOGLE_NOW.equals(i.getAction()) ) 
				&& i.getType() != null) {

			afterSavedReturnsToList = false;
			
			if (noteTmp == null) noteTmp = new Note();
			
			// Text title
			String title = i.getStringExtra(Intent.EXTRA_SUBJECT);
			if (title != null) {
				noteTmp.setTitle(title);
			}
			
			// Text content
			String content = i.getStringExtra(Intent.EXTRA_TEXT);
			if (content != null) {
				noteTmp.setContent(content);
			}
			
			// Single attachment data
			Uri uri = (Uri) i.getParcelableExtra(Intent.EXTRA_STREAM);
	    	// Due to the fact that Google Now passes intent as text but with 
	    	// audio recording attached the case must be handled in specific way
		    if (uri != null && !Constants.INTENT_GOOGLE_NOW.equals(i.getAction())) {
		    	String mimeType = StorageManager.getMimeTypeInternal(mActivity, i.getType());
		    	Attachment mAttachment = new Attachment(uri, mimeType);
		    	if (Constants.MIME_TYPE_FILES.equals(mimeType)) {
			    	mAttachment.setName(uri.getLastPathSegment());
		    	}
		    	noteTmp.addAttachment(mAttachment);
		    }
		    
		    // Multiple attachment data
		    ArrayList<Uri> uris = i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
		    if (uris != null) {
		    	Attachment mAttachment;
		    	for (Uri uriSingle : uris) {
		    		String mimeGeneral = StorageManager.getMimeType(mActivity, uriSingle);
		    		if (mimeGeneral != null) {
		    			String mimeType = StorageManager.getMimeTypeInternal(mActivity, mimeGeneral);
		    			mAttachment = new Attachment(uriSingle, mimeType);
				    	if (Constants.MIME_TYPE_FILES.equals(mimeType)) {
					    	mAttachment.setName(uriSingle.getLastPathSegment());
				    	}
				    	noteTmp.addAttachment(mAttachment);
		    		} else {
		    			mActivity.showToast(getString(R.string.error_importing_some_attachments), Toast.LENGTH_SHORT);
		    		}
				}
		    }
		}
		
	}

	
	private void initViews() {
				
		// Sets onTouchListener to the whole activity to swipe notes
		root = (ViewGroup) getView().findViewById(R.id.detail_root);
		root.setOnTouchListener(this);
		
		// Title view card container
		titleCardView = root.findViewById(R.id.detail_tile_card);
		
		// Overrides font sizes with the one selected from user
		Fonts.overrideTextSize(mActivity, prefs, root);

		// Color of tag marker if note is tagged a function is active in preferences
		setTagMarkerColor(noteTmp.getTag());		
		
		// Sets links clickable in title and content Views
		title = (EditText) getView().findViewById(R.id.detail_title);
		title.setText(noteTmp.getTitle());	
		title.gatherLinksForText();
		title.setOnTextLinkClickListener(this);
		
		content = (EditText) getView().findViewById(R.id.detail_content);
		content.setText(noteTmp.getContent());
		content.gatherLinksForText();
		content.setOnTextLinkClickListener(this);
		if (note.get_id() == 0 && !noteTmp.isChanged(note)) {			
			// Force focus and shows soft keyboard
			content.requestFocus();
//			InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
//	        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
			showKeyboard = true;
		}

		// Restore checklist
		toggleChecklistView = content;
		if (noteTmp.isChecklist()) {
			noteTmp.setChecklist(false);
//			toggleChecklistView.setVisibility(View.INVISIBLE);
			AlphaManager.setAlpha(toggleChecklistView, 0);
			toggleChecklist2();
		}
		
		// Initialization of location TextView
		locationTextView = (TextView) getView().findViewById(R.id.location);
//		if (mActivity.currentLatitude != 0 && mActivity.currentLongitude != 0) {
//			if (noteTmp.getAddress() != null && noteTmp.getAddress().length() > 0) {
//				locationTextView.setVisibility(View.VISIBLE);
//				locationTextView.setText(noteTmp.getAddress());
//			} else {
//				// Sets visibility now to avoid jumps on populating location
//				locationTextView.setVisibility(View.INVISIBLE);	
//				setAddress(locationTextView);
//			}
//		}
		if (noteTmp.getLatitude() != null && noteTmp.getLatitude() != 0 && noteTmp.getLongitude() != null
				&& noteTmp.getLongitude() != 0) {
			if (noteTmp.getAddress() != null && noteTmp.getAddress().length() > 0) {
				locationTextView.setVisibility(View.VISIBLE);
				locationTextView.setText(noteTmp.getAddress());
			} else {
				// Sets visibility now to avoid jumps on populating location
				resolveAddress(noteTmp.getLatitude(), noteTmp.getLongitude());
			}
		}

		locationTextView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String urlTag = Constants.TAG
						+ (noteTmp.getTitle() != null ? System.getProperty("line.separator") + noteTmp.getTitle() : "")
						+ (noteTmp.getContent() != null ? System.getProperty("line.separator") + noteTmp.getContent() : "");
				final String uriString = "http://maps.google.com/maps?q=" + noteTmp.getLatitude() + ',' + noteTmp.getLongitude() + "("
						+ urlTag + ")&z=15";
				Intent locationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
				startActivity(locationIntent);
			}
		});
		locationTextView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
				alertDialogBuilder.setMessage(R.string.remove_location)
						.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int id) {
								noteTmp.setLatitude("0");
								noteTmp.setLongitude("0");
								fade(locationTextView, false);
							}
						}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
				AlertDialog alertDialog = alertDialogBuilder.create();
				alertDialog.show();
				return true;
			}
		});

		

		// Some fields can be filled by third party application and are always
		// shown
		mGridView = (ExpandableHeightGridView) getView().findViewById(R.id.gridview);
		mAttachmentAdapter = new AttachmentAdapter((Activity)mActivity, noteTmp.getAttachmentsList(), mGridView);
		mAttachmentAdapter.setOnErrorListener(this);

		// Initialzation of gridview for images
		mGridView.setAdapter(mAttachmentAdapter);
		// mGridView.setExpanded(true);
		mGridView.autoresize();

		// Click events for images in gridview (zooms image)
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Attachment attachment = (Attachment) parent.getAdapter().getItem(position);
				Uri uri = attachment.getUri();
				Intent attachmentIntent = null;
				if (Constants.MIME_TYPE_IMAGE.equals(attachment.getMime_type())
						|| Constants.MIME_TYPE_SKETCH.equals(attachment.getMime_type())
						|| Constants.MIME_TYPE_FILES.equals(attachment.getMime_type())
					|| Constants.MIME_TYPE_VIDEO.equals(attachment.getMime_type())) {
					attachmentIntent = new Intent(Intent.ACTION_VIEW);
//					attachmentIntent.setDataAndType(uri, attachment.getMime_type());					
					attachmentIntent.setDataAndType(uri, StorageManager.getMimeType(mActivity, attachment.getUri()));
					attachmentIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
					if (IntentChecker.isAvailable(mActivity.getApplicationContext(), attachmentIntent, null)) {
						startActivity(attachmentIntent);
					} else {
						Crouton.makeText(mActivity, R.string.feature_not_available_on_this_device, ONStyle.WARN).show();
					}
					
				} else if (Constants.MIME_TYPE_AUDIO.equals(attachment.getMime_type())) {					
					playback(v, attachment.getUri()); 					
				}
				
			}
		});
		
		// Long click events for images in gridview (removes image)
		mGridView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View v, final int position, long id) {
				
				// To avoid deleting audio attachment during playback
				if (mPlayer != null) return false;
				
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
				alertDialogBuilder.setMessage(R.string.delete_selected_image)
						.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								noteTmp.getAttachmentsList().remove(position);
								mAttachmentAdapter.notifyDataSetChanged();
								mGridView.autoresize();
							}
						}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
				
				// If is an image user could want to sketch it!
				if (Constants.MIME_TYPE_SKETCH.equals(mAttachmentAdapter.getItem(position).getMime_type())) {
					alertDialogBuilder
						.setMessage(R.string.choose_action)
						.setNeutralButton(R.string.edit, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								takeSketch(mAttachmentAdapter.getItem(position));
							}
						});
				}
				
				AlertDialog alertDialog = alertDialogBuilder.create();
				alertDialog.show();
				return true;
			}
		});

		
		// Preparation for reminder icon
		reminder_layout = (LinearLayout) getView().findViewById(R.id.reminder_layout);
		reminder_layout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int pickerType = prefs.getBoolean("settings_simple_calendar", false) ? ReminderPickers.TYPE_AOSP : ReminderPickers.TYPE_GOOGLE;
				ReminderPickers reminderPicker = new ReminderPickers(mActivity, mFragment, pickerType);
				Long presetDateTime = noteTmp.getAlarm() != null ? Long.parseLong(noteTmp.getAlarm()) : null;
				reminderPicker.pick(presetDateTime);
				onDateSetListener = reminderPicker;
				onTimeSetListener = reminderPicker;
			}
		});
		reminder_layout.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
				alertDialogBuilder.setMessage(R.string.remove_reminder)
						.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int id) {
								alarmDate = "";
								alarmTime = "";
								noteTmp.setAlarm(null);
								datetime.setText("");
							}
						}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
				AlertDialog alertDialog = alertDialogBuilder.create();
				alertDialog.show();
				return true;
			}
		});

		
		// Reminder
		datetime = (TextView) getView().findViewById(R.id.datetime);
		datetime.setText(dateTimeText);
		
		// Timestamps view
		timestampsView = mActivity.findViewById(R.id.detail_timestamps);
		
		// Footer dates of creation... 
		TextView creationTextView = (TextView) getView().findViewById(R.id.creation);
		String creation = noteTmp.getCreationShort(mActivity);
		creationTextView.append(creation.length() > 0 ? getString(R.string.creation) + " "
				+ creation : "");
		if (creationTextView.getText().length() == 0)
			creationTextView.setVisibility(View.GONE);
		
		// ... and last modification
		TextView lastModificationTextView = (TextView) getView().findViewById(R.id.last_modification);
		String lastModification = noteTmp.getLastModificationShort(mActivity);
		lastModificationTextView.append(lastModification.length() > 0 ? getString(R.string.last_update) + " "
				+ lastModification : "");
		if (lastModificationTextView.getText().length() == 0)
			lastModificationTextView.setVisibility(View.GONE);
		
		// Adding a layout observer to perform calculus when showing keyboard
		root.getViewTreeObserver().addOnGlobalLayoutListener(this);
	}


	
	/**
	 * Colors tag marker in note title TextView
	 */
	private void setTagMarkerColor(Tag tag) {
		
		String colorsPref = prefs.getString("settings_colors_app", Constants.PREF_COLORS_APP_DEFAULT);
		
		// Checking preference
		if (!colorsPref.equals("disabled")){
			
			// Choosing target view depending on another preference
			ArrayList<View> target = new ArrayList<View>();
			if (colorsPref.equals("complete")){
				target.add(getView().findViewById(R.id.title_wrapper));
				target.add(getView().findViewById(R.id.content_wrapper));
			} else {
				target.add(getView().findViewById(R.id.tag_marker));
			}
			
			// Coloring the target
			if (tag != null && tag.getColor() != null) {
				for (View view : target) {
					view.setBackgroundColor(Integer.parseInt(tag.getColor()));
				}				
			} else {
				for (View view : target) {
					view.setBackgroundColor(Color.parseColor("#00000000"));
				}	
			}
		}
	}
	
	

	private void initNote() {

		// Workaround to get widget acting correctly
		if (note == null || note.get_id() == 0) {
			note = new Note();
		}
		
//		// Is a shared intent
//		if (note.get_id() == 0) {
//			note = new Note();
//		}
					
		if (noteTmp.getAlarm() != null) {
			dateTimeText = initAlarm(Long.parseLong(noteTmp.getAlarm()));
		}
		
//		if (noteTmp.getLatitude() != null && noteTmp.getLongitude() != null) {
//			mActivity.currentLatitude = noteTmp.getLatitude() != null ? noteTmp.getLatitude() : null ;
//			mActivity.currentLongitude = noteTmp.getLongitude() != null ? noteTmp.getLatitude() : null;
//		}
	}

	
	@SuppressLint("NewApi")
	private void setAddress() {
		double lat = mActivity.currentLatitude;
		double lon = mActivity.currentLongitude;
		noteTmp.setLatitude(lat);
		noteTmp.setLongitude(lon);
		resolveAddress(lat, lon);
	}
	
	
	
	@SuppressLint("NewApi")
	private void resolveAddress(double lat, double lon) {
		class LocatorTask extends AsyncTask<Void, Void, String> {
			
			private final String ERROR_MSG = getString(R.string.location_not_found);
			private TextView mlocationTextView;
			private double lat, lon;

			public LocatorTask(TextView locationTextView, double lat, double lon) {
				this.mlocationTextView = locationTextView;
				this.lat = lat;
				this.lon = lon;
				
			}

			@Override
			protected String doInBackground(Void... params) {
				
				String addressString = "";
				try {
					Geocoder gcd = new Geocoder(mActivity, Locale.getDefault());
					List<Address> addresses = gcd.getFromLocation(this.lat, this.lon, 1);
					if (addresses.size() > 0) {
						Address address = addresses.get(0);
						if (address != null) {
							addressString = address.getThoroughfare() + ", " + address.getLocality();
						} else {
							addressString = ERROR_MSG;
						}
					} else {
						addressString = ERROR_MSG;
					}
				} catch (IOException ex) {
					addressString = ERROR_MSG;
				}
				return addressString;
			}

			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
				if (result.length() > 0 && !result.equals(ERROR_MSG)) {
					this.mlocationTextView.setVisibility(View.VISIBLE);
					this.mlocationTextView.setText(result);
					fade(mlocationTextView, true);
				} else {
					Crouton.makeText(mActivity, ERROR_MSG, ONStyle.ALERT).show();
				}				
			}
		}

		LocatorTask task = new LocatorTask(locationTextView, lat, lon);		
		if (Build.VERSION.SDK_INT >= 11) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			task.execute();
		}
	}
	
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		inflater.inflate(R.menu.menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	    
	    // Show instructions on first launch
	    final String instructionName = Constants.PREF_TOUR_PREFIX + "detail";
	    if (AppTourHelper.isMyTurn(mActivity, instructionName)
	    		&& !onCreateOptionsMenuAlreadyCalled ) {			
	    	onCreateOptionsMenuAlreadyCalled = true;
			ArrayList<Integer[]> list = new ArrayList<Integer[]>();
			list.add(new Integer[]{R.id.menu_attachment, R.string.tour_detailactivity_attachment_title, R.string.tour_detailactivity_attachment_detail, ShowcaseView.ITEM_ACTION_ITEM});
			list.add(new Integer[]{R.id.menu_tag, R.string.tour_detailactivity_action_title, R.string.tour_detailactivity_action_detail, ShowcaseView.ITEM_ACTION_ITEM});
			list.add(new Integer[]{R.id.datetime, R.string.tour_detailactivity_reminder_title, R.string.tour_detailactivity_reminder_detail, null});
			list.add(new Integer[]{R.id.detail_title, R.string.tour_detailactivity_links_title, R.string.tour_detailactivity_links_detail, null});
			list.add(new Integer[]{null, R.string.tour_detailactivity_swipe_title, R.string.tour_detailactivity_swipe_detail, null, -10, Display.getUsableSize(mActivity).y/3, 80, Display.getUsableSize(mActivity).y/3});
			list.add(new Integer[]{0, R.string.tour_detailactivity_save_title, R.string.tour_detailactivity_save_detail, ShowcaseView.ITEM_ACTION_HOME});
			mActivity.showCaseView(list, new OnShowcaseAcknowledged() {			
				@Override
				public void onShowCaseAcknowledged(ShowcaseView showcaseView) {
					prefs.edit().putBoolean(instructionName, true).commit();
					discard();
				}
			});	
	    }
	}

	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.menu_share).setVisible(true);
		menu.findItem(R.id.menu_attachment).setVisible(true);
		menu.findItem(R.id.menu_tag).setVisible(true);
		menu.findItem(R.id.menu_checklist_on).setVisible(!noteTmp.isChecklist());
		menu.findItem(R.id.menu_checklist_off).setVisible(noteTmp.isChecklist());
		menu.findItem(R.id.menu_lock).setVisible(!noteTmp.isLocked());
		menu.findItem(R.id.menu_unlock).setVisible(noteTmp.isLocked());
		menu.findItem(R.id.menu_add_shortcut).setVisible(noteTmp.get_id() != 0);
		menu.findItem(R.id.menu_delete).setVisible(true);
		menu.findItem(R.id.menu_discard_changes).setVisible(true);
		menu.findItem(R.id.menu_archive).setVisible(!noteTmp.isArchived());
		menu.findItem(R.id.menu_unarchive).setVisible(noteTmp.isArchived());
	}

	
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public boolean goHome() {
		stopPlaying();
		
		// Hides keyboard
	    KeyboardUtils.hideKeyboard(title);

	    String msg = resultIntent.getStringExtra(Constants.INTENT_DETAIL_RESULT_MESSAGE);
	    
		// The activity has managed a shared intent from third party app and
		// performs a normal onBackPressed instead of returning back to ListActivity
		if (!afterSavedReturnsToList) {
			if (!TextUtils.isEmpty(msg)) {
				mActivity.showToast(msg, Toast.LENGTH_SHORT);
			}
			mActivity.finish();
			return true;
		} else {
			if (!TextUtils.isEmpty(msg)) {
				Crouton.makeText(mActivity, msg, ONStyle.CONFIRM).show();
			}
		}
		
		// Unregistering layout observer
		if (root != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			} else {
				root.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}	
		}
		
		// Otherwise the result is passed to ListActivity
//		mActivity.loadNotesSync = true;
		mActivity.getSupportFragmentManager().popBackStack(); 
		if (mActivity.getSupportFragmentManager().getBackStackEntryCount() == 1) {
			mActivity.getSupportActionBar().setDisplayShowTitleEnabled(true);
		}
		if (mActivity.getDrawerToggle() != null) {
			mActivity.getDrawerToggle().setDrawerIndicatorEnabled(true);
		}
		
		return true;
	}

	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			afterSavedReturnsToList = true;
			saveNote(null);
			break;
		case R.id.menu_archive:
			saveNote(true);
			break;
		case R.id.menu_unarchive:
			saveNote(false);
			break;
		case R.id.menu_share:
			shareNote();
			break;
		case R.id.menu_attachment:
			showPopup(mActivity.findViewById(R.id.menu_attachment));
			break;
		case R.id.menu_tag:
			tagNote();
			break;
		case R.id.menu_checklist_on:
			toggleChecklist();
			break;
		case R.id.menu_checklist_off:
			toggleChecklist();
			break;
		case R.id.menu_lock:
			lockNote();
			break;
		case R.id.menu_unlock:
			lockNote();
			break;
		case R.id.menu_add_shortcut:
			addShortcut();
			break;
		case R.id.menu_delete:
			deleteNote();
			break;
		case R.id.menu_discard_changes:
			discard();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 
	 */
	private void toggleChecklist() {
		
		// In case checklist is active a prompt will ask about many options
		// to decide hot to convert back to simple text	
		if (!noteTmp.isChecklist()) {
			toggleChecklist2();
			return;
		}

		// If checklist is active but no items are checked the conversion in done automatically
		// without prompting user
		if (mChecklistManager.getCheckedStatus(toggleChecklistView)[0] == 0) {
			toggleChecklist2(true, false);
			return;
		}
		
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);

		// Inflate the popup_layout.xml
		LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.dialog_remove_checklist_layout, (ViewGroup) getView().findViewById(R.id.layout_root));

		// Retrieves options checkboxes and initialize their values
		final CheckBox keepChecked = (CheckBox) layout.findViewById(R.id.checklist_keep_checked);
		final CheckBox keepCheckmarks = (CheckBox) layout.findViewById(R.id.checklist_keep_checkmarks);		
		keepChecked.setChecked(prefs.getBoolean(Constants.PREF_KEEP_CHECKED, true));
		keepCheckmarks.setChecked(prefs.getBoolean(Constants.PREF_KEEP_CHECKMARKS, true));
		
		alertDialogBuilder.setView(layout)
		.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				prefs.edit()
					.putBoolean(Constants.PREF_KEEP_CHECKED, keepChecked.isChecked())
					.putBoolean(Constants.PREF_KEEP_CHECKMARKS, keepCheckmarks.isChecked())
					.commit();
				
				toggleChecklist2();
				dialog.dismiss();
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();				
			}
		});
		alertDialogBuilder.create().show();
	}
	
	
	/**
	 * Toggles checklist view
	 */
	private void toggleChecklist2() {
		boolean keepChecked =prefs.getBoolean(Constants.PREF_KEEP_CHECKED, true);
		boolean showChecks = prefs.getBoolean(Constants.PREF_KEEP_CHECKMARKS, true);
		toggleChecklist2(keepChecked, showChecks);
	}
	
	
	@SuppressLint("NewApi")
	private void toggleChecklist2(final boolean keepChecked, final boolean showChecks) {
		
		// AsyncTask processing doesn't work on some OS versions because in native classes
		// (maybe TextView) another thread is launched and this brings to the folowing error:
		// java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()
		
//		class ChecklistTask extends AsyncTask<Void, Void, View> {
//			private View targetView;
//			
//			public ChecklistTask(View targetView) {
//				this.targetView = targetView;
//			}
//			
//			@Override
//			protected View doInBackground(Void... params) {
//				
//				// Get instance and set options to convert EditText to CheckListView
//				mChecklistManager = ChecklistManager.getInstance(mActivity);
//				mChecklistManager.setMoveCheckedOnBottom(Integer.valueOf(prefs.getString("settings_checked_items_behavior",
//						String.valueOf(it.feio.android.checklistview.interfaces.Constants.CHECKED_HOLD))));
//				mChecklistManager.setShowChecks(true);
//				mChecklistManager.setNewEntryHint(getString(R.string.checklist_item_hint));
//				// Set the textChangedListener on the replaced view
//				mChecklistManager.setCheckListChangedListener(mFragment);
//				mChecklistManager.addTextChangedListener(mFragment);
//				
//				// Links parsing options
//				mChecklistManager.setOnTextLinkClickListener(mFragment);
//				
//				// Options for converting back to simple text
//				mChecklistManager.setKeepChecked(keepChecked);
//				mChecklistManager.setShowChecks(showChecks);
//				
//				// Switches the views
//				View newView = null;
//				try {
//					newView = mChecklistManager.convert(this.targetView);								
//				} catch (ViewNotSupportedException e) {
//					Log.e(Constants.TAG, "Error switching checklist view", e);
//				}
//				
//				return newView;
//			}
//			
//			@Override
//			protected void onPostExecute(View newView) {
//				super.onPostExecute(newView);
//				// Switches the views	
//				if (newView != null) {
//					mChecklistManager.replaceViews(this.targetView, newView);
//					toggleChecklistView = newView;					
////					fade(toggleChecklistView, true);
//					animate(this.targetView).alpha(1).scaleXBy(0).scaleX(1).scaleYBy(0).scaleY(1);
//					noteTmp.setChecklist(!noteTmp.isChecklist());
//				}				
//			}
//		}
//		
//		ChecklistTask task = new ChecklistTask(toggleChecklistView);		
//		if (Build.VERSION.SDK_INT >= 11) {
//			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//		} else {
//			task.execute();
//		}
		
		

		// Get instance and set options to convert EditText to CheckListView
		mChecklistManager = ChecklistManager.getInstance(mActivity);
		mChecklistManager.setMoveCheckedOnBottom(Integer.valueOf(prefs.getString("settings_checked_items_behavior",
				String.valueOf(it.feio.android.checklistview.interfaces.Constants.CHECKED_HOLD))));
		mChecklistManager.setShowChecks(true);
		mChecklistManager.setNewEntryHint(getString(R.string.checklist_item_hint));
		
		// Links parsing options
		mChecklistManager.setOnTextLinkClickListener(mFragment);
		
		// Options for converting back to simple text
		mChecklistManager.setKeepChecked(keepChecked);
		mChecklistManager.setShowChecks(showChecks);
		
		// Switches the views
		View newView = null;
		try {
			newView = mChecklistManager.convert(toggleChecklistView);								
		} catch (ViewNotSupportedException e) {
			Log.e(Constants.TAG, "Error switching checklist view", e);
		}
			
		// Switches the views	
		if (newView != null) {
			mChecklistManager.replaceViews(toggleChecklistView, newView);
			toggleChecklistView = newView;					
//			fade(toggleChecklistView, true);
			animate(toggleChecklistView).alpha(1).scaleXBy(0).scaleX(1).scaleYBy(0).scaleY(1);
			noteTmp.setChecklist(!noteTmp.isChecklist());
		}		
	}
	
	

	/**
	 * Tags note choosing from a list of previously created tags
	 */
	private void tagNote() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);

		// Retrieves all available tags
		final ArrayList<Tag> tags = db.getTags();
		
		alertDialogBuilder.setTitle(R.string.tag_as)
							.setAdapter(new NavDrawerTagAdapter(mActivity, tags), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									noteTmp.setTag(tags.get(which));
									setTagMarkerColor(tags.get(which));
								}
							}).setPositiveButton(R.string.add_tag, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {
									Intent intent = new Intent(mActivity, TagActivity.class);		
									intent.putExtra("noHome", true);
									startActivityForResult(intent, TAG);
								}
							}).setNeutralButton(R.string.remove_tag, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {
									noteTmp.setTag(null);
									setTagMarkerColor(null);
								}
							}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {
								}
							});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();		
	}
	
	
	
	
	 
	// The method that displays the popup.
	private void showPopup(View anchor) {
		DisplayMetrics metrics = new DisplayMetrics();
		mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		// Inflate the popup_layout.xml
		LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.attachment_dialog, null);

		// Creating the PopupWindow
		attachmentDialog = new PopupWindow(mActivity);
		attachmentDialog.setContentView(layout);
		attachmentDialog.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		attachmentDialog.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		attachmentDialog.setFocusable(true);
		attachmentDialog.setOnDismissListener(new OnDismissListener() {			
			@Override
			public void onDismiss() {
				if (isRecording) {
					isRecording = false;
					stopRecording();
				}
			}
		});

		// Clear the default translucent background
		attachmentDialog.setBackgroundDrawable(new BitmapDrawable());

		// Camera
		android.widget.TextView cameraSelection = (android.widget.TextView) layout.findViewById(R.id.camera);
		cameraSelection.setOnClickListener(new AttachmentOnClickListener());
		// Gallery
		android.widget.TextView gallerySelection = (android.widget.TextView) layout.findViewById(R.id.gallery);
		gallerySelection.setOnClickListener(new AttachmentOnClickListener());
		// Audio recording
		android.widget.TextView recordingSelection = (android.widget.TextView) layout.findViewById(R.id.recording);
		recordingSelection.setOnClickListener(new AttachmentOnClickListener());
		// Video recording
		android.widget.TextView videoSelection = (android.widget.TextView) layout.findViewById(R.id.video);
		videoSelection.setOnClickListener(new AttachmentOnClickListener());
		// Files
		android.widget.TextView filesSelection = (android.widget.TextView) layout.findViewById(R.id.files);
		filesSelection.setOnClickListener(new AttachmentOnClickListener());
		// Sketch
		android.widget.TextView sketchSelection = (android.widget.TextView) layout.findViewById(R.id.sketch);
		sketchSelection.setOnClickListener(new AttachmentOnClickListener());
		// Location
		android.widget.TextView locationSelection = (android.widget.TextView) layout.findViewById(R.id.location);
		locationSelection.setOnClickListener(new AttachmentOnClickListener());
		
		try {
			attachmentDialog.showAsDropDown(anchor);
		} catch (Exception e){
			Crouton.makeText(mActivity, R.string.error, ONStyle.ALERT).show();			
		}
	}

	
	/**
	 * Manages clicks on attachment dialog
	 */
	@SuppressLint("InlinedApi")
	private class AttachmentOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			// Photo from camera
			case R.id.camera:
				takePhoto();
				attachmentDialog.dismiss();
				break;
			// Image from gallery
			case R.id.gallery:
				Intent galleryIntent;
				if (Build.VERSION.SDK_INT >= 19) {
//					galleryIntent = new Intent(Intent.ACTION_PICK,
//							android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					takeGalleryKitKat();
				} else {
					galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
					galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
					galleryIntent.setType("*/*");
					startActivityForResult(galleryIntent, GALLERY);
				}
				attachmentDialog.dismiss();
				break;
			// Microphone recording
			case R.id.recording:
				if (!isRecording) {
					isRecording = true;
					android.widget.TextView mTextView = (android.widget.TextView) v;
					mTextView.setText(getString(R.string.stop));
					mTextView.setTextColor(Color.parseColor("#ff0000"));
					startRecording();
				} else {
					isRecording = false;
					stopRecording();
					Attachment attachment = new Attachment(Uri.parse(recordName), Constants.MIME_TYPE_AUDIO);
					attachment.setMoveWhenNoteSaved(false);
					attachment.setLength(audioRecordingTime);
					noteTmp.getAttachmentsList().add(attachment);
					mAttachmentAdapter.notifyDataSetChanged();
					mGridView.autoresize();					
					// Updates sharing intent after attachment insertion
//					updateShareIntent();
					attachmentDialog.dismiss();
				}
				break;
			case R.id.video:
				takeVideo();
				attachmentDialog.dismiss();
			    break;
			case R.id.files:
				Intent filesIntent;
				filesIntent = new Intent(Intent.ACTION_GET_CONTENT);
//				filesIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
				filesIntent.addCategory(Intent.CATEGORY_OPENABLE);
				filesIntent.setType("*/*");
				startActivityForResult(filesIntent, FILES);
				attachmentDialog.dismiss();
				break;
			case R.id.sketch:
				takeSketch(null);
				attachmentDialog.dismiss();
			    break;
			case R.id.location:
				setAddress();
				attachmentDialog.dismiss();
				break;
			}	
		}
	}


	/**
	 * Shows a dialog to choose between image or video attachment for storage access framework app
	 */
	@TargetApi(19)
	private void takeGalleryKitKat() {
		
		final Intent intent = new Intent();
		intent.setAction(Intent.ACTION_GET_CONTENT);
//		intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
		
		ArrayList<ImageAndTextItem> options = new ArrayList<ImageAndTextItem>();
		options.add(new ImageAndTextItem(R.drawable.ic_photo_dark, getString(R.string.image)) );
		options.add(new ImageAndTextItem(R.drawable.ic_action_video, getString(R.string.video)) );
		
		alertDialogBuilder
		.setAdapter(new ImageAndTextAdapter(mActivity, options), new DialogInterface.OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case 0:
					intent.setType("image/*");
					startActivityForResult(intent, GALLERY);
					break;
				case 1:
					intent.setType("video/*");
					startActivityForResult(intent, GALLERY);
					break;
				}				
			}
		});
		
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}	
	

	
	private void takePhoto() {
		File f = StorageManager.createNewAttachmentFile(mActivity, Constants.MIME_TYPE_IMAGE_EXT);
		if (f == null) {
			Crouton.makeText(mActivity, R.string.error, ONStyle.ALERT).show();
			return;
		}
		attachmentUri = Uri.fromFile(f);		
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, attachmentUri);
		startActivityForResult(intent, TAKE_PHOTO);
	}
	
	
	private void takeVideo() {
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		if (!IntentChecker.isAvailable(mActivity, takeVideoIntent, new String[]{PackageManager.FEATURE_CAMERA})) {
			Crouton.makeText(mActivity, R.string.feature_not_available_on_this_device, ONStyle.ALERT).show();
			return;
		}		
		// File is stored in custom ON folder to speedup the attachment 
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
			File f = StorageManager.createNewAttachmentFile(mActivity, Constants.MIME_TYPE_VIDEO_EXT);
			if (f == null) {
				Crouton.makeText(mActivity, R.string.error, ONStyle.ALERT).show();
				return;
			}
			attachmentUri = Uri.fromFile(f);	
			takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, attachmentUri);
		}
		String maxVideoSizeStr = "".equals(prefs.getString("settings_max_video_size", "")) ? "0" : prefs.getString("settings_max_video_size", "");
		int maxVideoSize = Integer.parseInt(maxVideoSizeStr);
		takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, Long.valueOf(maxVideoSize*1024*1024));
	    startActivityForResult(takeVideoIntent, TAKE_VIDEO);
	}
	
	
	private void takeSketch(Attachment attachment) {
		File f = StorageManager.createNewAttachmentFile(mActivity, Constants.MIME_TYPE_SKETCH_EXT);
		if (f == null) {
			Crouton.makeText(mActivity, R.string.error, ONStyle.ALERT).show();
			return;
		}
		attachmentUri = Uri.fromFile(f);	
		
		// Fragments replacing
		FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
		mActivity.animateTransition(transaction, mActivity.TRANSITION_HORIZONTAL);
		SketchFragment mSketchFragment = new SketchFragment();
		Bundle b = new Bundle();
		b.putParcelable(MediaStore.EXTRA_OUTPUT, attachmentUri);
		if (attachment != null) {
			b.putParcelable("base", attachment.getUri());
		}
		mSketchFragment.setArguments(b);
		transaction.replace(R.id.fragment_container, mSketchFragment, mActivity.FRAGMENT_SKETCH_TAG).addToBackStack(mActivity.FRAGMENT_DETAIL_TAG).commit();
	}

	
	@SuppressLint("NewApi")
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// Fetch uri from activities, store into adapter and refresh adapter
		Attachment attachment;
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case TAKE_PHOTO:
				attachment = new Attachment(attachmentUri, Constants.MIME_TYPE_IMAGE);
				attachment.setMoveWhenNoteSaved(false);
				noteTmp.getAttachmentsList().add(attachment);
				mAttachmentAdapter.notifyDataSetChanged();
				mGridView.autoresize();
				break;
			case GALLERY:
				Uri uri = intent.getData();
				AttachmentTask task = new AttachmentTask(this, uri, this);
				task.execute();
				break;
			case TAKE_VIDEO:
				// Gingerbread doesn't allow custom folder so data are retrieved from intent 
				if(Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
					attachment = new Attachment(attachmentUri, Constants.MIME_TYPE_VIDEO);
					attachment.setMoveWhenNoteSaved(false);
				} else {
					attachment = new Attachment(intent.getData(), Constants.MIME_TYPE_VIDEO);
				}
				noteTmp.getAttachmentsList().add(attachment);
				mAttachmentAdapter.notifyDataSetChanged();
				mGridView.autoresize();
				break;
			case FILES:
				if (resultCode == Activity.RESULT_OK) {
					Uri filesUri = intent.getData();
					String name = FileHelper.getNameFromUri(mActivity, filesUri);					
					AttachmentTask task1 = new AttachmentTask(this, filesUri, name, this);
					task1.execute();
					
				} else {
					Crouton.makeText(mActivity,
							R.string.error_saving_attachments, ONStyle.ALERT)
							.show();
				}
				break;
			case SET_PASSWORD:
				noteTmp.setPasswordChecked(true);
				lockUnlock();
				break;
			case SKETCH:
				attachment = new Attachment(attachmentUri, Constants.MIME_TYPE_SKETCH);
				attachment.setMoveWhenNoteSaved(false);
				noteTmp.getAttachmentsList().add(attachment);
				mAttachmentAdapter.notifyDataSetChanged();
				mGridView.autoresize();
				break;
			case TAG:
				Crouton.makeText(mActivity, R.string.tag_saved,
						ONStyle.CONFIRM).show();
				Tag tag = intent.getParcelableExtra("tag");
				noteTmp.setTag(tag);
				setTagMarkerColor(tag);
				break;
			case DETAIL:
				Crouton.makeText(mActivity, R.string.note_updated,
						ONStyle.CONFIRM).show();				
				break;
			}
		}
	}


	@Override
	public void onSketchSaved(Uri attachmentUri) {
		Attachment attachment = new Attachment(attachmentUri, Constants.MIME_TYPE_SKETCH);
		attachment.setMoveWhenNoteSaved(false);
		noteTmp.getAttachmentsList().add(attachment);
		mAttachmentAdapter.notifyDataSetChanged();
		mGridView.autoresize();
	}
	
		
	/**
	 * Discards changes done to the note and eventually delete new attachments
	 */
	private void discard() {
		// Checks if some new files have been attached and must be removed
		if (!noteTmp.getAttachmentsList().equals(note.getAttachmentsList())) {
			for (Attachment newAttachment: noteTmp.getAttachmentsList()) {
				if (!note.getAttachmentsList().contains(newAttachment) && !newAttachment.getMoveWhenNoteSaved()) {
					StorageManager.delete(mActivity, newAttachment.getUri().getPath());
				}
			}
		}
		goHome();
	}
	
	

	@SuppressLint("NewApi")
	private void deleteNote() {

		// Confirm dialog creation
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
		alertDialogBuilder.setMessage(R.string.delete_note_confirmation)
				.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
						// Simply return to the previous
						// activity/fragment if it was a new note
						if (noteTmp.get_id() == 0) {
							goHome();
							return;
						}

						// Saving changes to the note
						DeleteNoteTask deleteNoteTask = new DeleteNoteTask(mActivity);
						// Forceing parallel execution disabled by default
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
							deleteNoteTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, note);
						} else {
							deleteNoteTask.execute(note);
						}

						// Informs the user about update
						Log.d(Constants.TAG, "Deleted note with id '" + noteTmp.get_id() + "'");
						Crouton.makeText(mActivity, getString(R.string.note_deleted), ONStyle.ALERT).show();
						
						MainActivity.notifyAppWidgets(mActivity);
						
						goHome();
						return;
					}
				}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
					}
				});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	/**
	 * Save new notes, modify them or archive
	 * 
	 * @param archive
	 *            Boolean flag used to archive note. If null actual note state is used.
	 */

	@TargetApi(Build.VERSION_CODES.HONEYCOMB) void saveNote(Boolean archive) {
		// Changed fields
		noteTmp.setTitle(getNoteTitle());
		noteTmp.setContent(getNoteContent());	
		noteTmp.setArchived(archive == null ? noteTmp.isArchived() : archive);
		
		// Check if some text or attachments of any type have been inserted or
		// is an empty note
		if (noteTmp.isEmpty()) {
			Log.d(Constants.TAG, "Empty note not saved");
//			resultIntent.putExtra(Constants.INTENT_DETAIL_RESULT_CODE, Activity.RESULT_FIRST_USER);
//			resultIntent.putExtra(Constants.INTENT_DETAIL_RESULT_MESSAGE, getString(R.string.empty_note_not_saved));
			Crouton.makeText(mActivity, getString(R.string.empty_note_not_saved), ONStyle.INFO).show();
			goHome();
			return;
		}
		
		// Checks if nothing is changed to avoid committing if possible (check)
		if (!noteTmp.isChanged(note)) {
			goHome();
			return;
		}		
		
		// Checks if only tag has been changed and then force to not update 
		// last modification date
		boolean updateLastModification = true;
		note.setTag(noteTmp.getTag());
		if (!noteTmp.isChanged(note)) {
			updateLastModification = false;
		}		
		
		noteTmp.setAttachmentsListOld(note.getAttachmentsList());

		// Saving changes to the note
		SaveNoteTask saveNoteTask = new SaveNoteTask(this, updateLastModification);
		// Forcing parallel execution disabled by default
		if (Build.VERSION.SDK_INT >= 11) {
			saveNoteTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, noteTmp);
		} else {
			saveNoteTask.execute(noteTmp);
		}

		resultIntent.putExtra(Constants.INTENT_DETAIL_RESULT_MESSAGE, getString(R.string.note_updated));
//		if (afterSavedReturnsToList) {
//			Crouton.makeText(mActivity, getString(R.string.note_updated), ONStyle.CONFIRM).show();
//		}
		
		MainActivity.notifyAppWidgets(mActivity);
	}
	


	private String getNoteTitle() {
		String res = "";
		if (getActivity().findViewById(R.id.detail_title) != null) {
			res = ((EditText) getActivity().findViewById(R.id.detail_title)).getText().toString();
		}
		return res;
	}
	
	
	private String getNoteContent() {
		String content = "";
		if (!noteTmp.isChecklist()) {
			// Due to checklist library introduction the returned EditText class is no more
			// a com.neopixl.pixlui.components.edittext.EditText but a standard
			// android.widget.EditText
			try {
				try {
					content = ((EditText) getActivity().findViewById(R.id.detail_content)).getText().toString();
				} catch (ClassCastException e) {
					content = ((android.widget.EditText)  getActivity().findViewById(R.id.detail_content)).getText().toString();
				}
			}catch (NullPointerException e) {}
		} else {
				if (mChecklistManager != null) {
					mChecklistManager.setKeepChecked(true);
					mChecklistManager.setShowChecks(true);
					content = mChecklistManager.getText((CheckListView)toggleChecklistView);
				}
		}
		return content;
	}


	
	/**
	 * Updates share intent 
	 */
	private void shareNote() {		
		Note sharedNote = new Note(noteTmp);
		sharedNote.setTitle(getNoteTitle());
		sharedNote.setContent(getNoteContent());
		mActivity.shareNote(sharedNote);
	}
	
	
	
	/**
	 * Notes locking with security password to avoid viewing, editing or deleting from unauthorized
	 */
	private void lockNote() {
		Log.d(Constants.TAG, "Locking or unlocking note " + note.get_id());
		
		// If security password is not set yes will be set right now
		if (prefs.getString(Constants.PREF_PASSWORD, null) == null) {
			Intent passwordIntent = new Intent(mActivity, PasswordActivity.class);
			startActivityForResult(passwordIntent, SET_PASSWORD);
			return;
		}
		
		// If password has already been inserted will not be asked again
		if (noteTmp.isPasswordChecked()) {
			lockUnlock();
			return;
		}
		
		// Password will be requested here
		mActivity.requestPassword(new PasswordValidator() {					
			@Override
			public void onPasswordValidated(boolean result) {
				if (result) {
					lockUnlock();
				} 
			}
		});
	}
	
	
	private void lockUnlock(){
		if (noteTmp.isLocked()) {
			Crouton.makeText(mActivity, R.string.save_note_to_unlock_it, ONStyle.INFO).show();
			mActivity.supportInvalidateOptionsMenu();
		} else {
			Crouton.makeText(mActivity, R.string.save_note_to_lock_it, ONStyle.INFO).show();
			mActivity.supportInvalidateOptionsMenu();
		}
		noteTmp.setLocked(!noteTmp.isLocked());
	}
	
	

	/**
	 * Used to set actual alarm state when initializing a note to be edited
	 * 
	 * @param alarmDateTime
	 * @return
	 */
	private String initAlarm(long alarmDateTime) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(alarmDateTime);
		alarmDate = DateHelper.onDateSet(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
				cal.get(Calendar.DAY_OF_MONTH), Constants.DATE_FORMAT_SHORT_DATE);
		alarmTime = DateHelper.getTimeShort(mActivity, cal.getTimeInMillis());
		String dateTimeText = getString(R.string.alarm_set_on) + " " + alarmDate + " " + getString(R.string.at_time)
				+ " " + alarmTime;
		return dateTimeText;
	}

	public String getAlarmDate() {
		return alarmDate;
	}

	public String getAlarmTime() {
		return alarmTime;
	}



	/**
	 * Audio recordings playback
	 * @param v
	 * @param uri
	 */
	private void playback(View v, Uri uri) {
		// Some recording is playing right now
		if (mPlayer != null && mPlayer.isPlaying()) {
			// If the audio actually played is NOT the one from the click view the last one is played
			if (isPlayingView != v) {
				stopPlaying();
				isPlayingView = v;
				startPlaying(uri);
				recordingBitmap = ((BitmapDrawable)((ImageView)v.findViewById(R.id.gridview_item_picture)).getDrawable()).getBitmap();
				((ImageView)v.findViewById(R.id.gridview_item_picture)).setImageBitmap(ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.stop), Constants.THUMBNAIL_SIZE, Constants.THUMBNAIL_SIZE));
			// Otherwise just stops playing
			} else {			
				stopPlaying();	
			}
		// If nothing is playing audio just plays	
		} else {
			isPlayingView = v;
			startPlaying(uri);	
			Drawable d = ((ImageView)v.findViewById(R.id.gridview_item_picture)).getDrawable();
			if (BitmapDrawable.class.isAssignableFrom(d.getClass())) {
				recordingBitmap = ((BitmapDrawable)d).getBitmap();
			} else {
				recordingBitmap = ((BitmapDrawable)((TransitionDrawable)d).getDrawable(1)).getBitmap();
			}
			((ImageView)v.findViewById(R.id.gridview_item_picture)).setImageBitmap(ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.stop), Constants.THUMBNAIL_SIZE, Constants.THUMBNAIL_SIZE));
//			((ImageView)v).setImageBitmap(ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.stop), mGridView.getItemHeight(), mGridView.getItemHeight()));
			}
	}
	
	private void startPlaying(Uri uri) {
		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(mActivity, uri);
			mPlayer.prepare();
			mPlayer.start();
			mPlayer.setOnCompletionListener(new OnCompletionListener() {
				
				@Override
				public void onCompletion(MediaPlayer mp) {
					mPlayer = null;
					((ImageView)isPlayingView.findViewById(R.id.gridview_item_picture)).setImageBitmap(recordingBitmap);
					recordingBitmap = null;
					isPlayingView = null;
				}
			});
		} catch (IOException e) {
			Log.e(Constants.TAG, "prepare() failed");
		}
	}

	private void stopPlaying() {
		if (mPlayer != null) {
			((ImageView)isPlayingView.findViewById(R.id.gridview_item_picture)).setImageBitmap(recordingBitmap);
			isPlayingView = null;
			recordingBitmap = null;
			mPlayer.release();
			mPlayer = null;
		}
	}

	private void startRecording() {
		File f = StorageManager.createNewAttachmentFile(mActivity, Constants.MIME_TYPE_AUDIO_EXT);
		if (f == null) {
			Crouton.makeText(mActivity, R.string.error, ONStyle.ALERT).show();
			return;
		}
		recordName = f.getAbsolutePath();
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		mRecorder.setAudioEncodingBitRate(16);
//		mRecorder.setAudioSamplingRate(44100);
		mRecorder.setOutputFile(recordName);

		try {
			mRecorder.prepare();
			audioRecordingTimeStart = Calendar.getInstance().getTimeInMillis();
			mRecorder.start();
		} catch (IOException e) {
			Log.e(Constants.TAG, "prepare() failed");
		}
	}

	private void stopRecording() {
		if (mRecorder!= null) {
			mRecorder.stop();
			audioRecordingTime = Calendar.getInstance().getTimeInMillis() - audioRecordingTimeStart; 
			mRecorder.release();
			mRecorder = null;
		}
	}
	


	private void fade(final View v, boolean fadeIn) {
		
		int anim = R.animator.fade_out;
		int visibilityTemp = View.GONE;
		
		if (fadeIn) {
			anim = R.animator.fade_in;
			visibilityTemp = View.VISIBLE;
		}
		
		final int visibility = visibilityTemp;
		
		if (prefs.getBoolean("settings_enable_animations", true)) {
			Animation mAnimation = AnimationUtils.loadAnimation(mActivity, anim);
			mAnimation.setAnimationListener(new AnimationListener() {				
				@Override
				public void onAnimationStart(Animation animation) {}			
				@Override
				public void onAnimationRepeat(Animation animation) {}				
				@Override
				public void onAnimationEnd(Animation animation) {
					v.setVisibility(visibility);
				}
			});
			v.startAnimation(mAnimation);
		}
	}
	
	
	/**
	 * Adding shortcut on Home screen
	 */
	private void addShortcut() {
		Intent shortcutIntent = new Intent(mActivity, MainActivity.class);
		shortcutIntent.putExtra(Constants.INTENT_KEY, noteTmp.get_id());
		shortcutIntent.setAction(Constants.ACTION_SHORTCUT);
		
		Intent addIntent = new Intent();
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		String shortcutTitle = note.getTitle().length() > 0 ? note.getTitle() : note.getCreationShort(mActivity);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutTitle);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(mActivity, R.drawable.ic_stat_notification_icon));
		addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		
		mActivity.sendBroadcast(addIntent);
		Crouton.makeText(mActivity, R.string.shortcut_added, ONStyle.INFO).show();		
	}


	
	/* (non-Javadoc)
	 * @see com.neopixl.pixlui.links.TextLinkClickListener#onTextLinkClick(android.view.View, java.lang.String, java.lang.String)
	 * 
	 * Receives onClick from links in EditText and shows a dialog to open link or copy content
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onTextLinkClick(View view, final String clickedString, final String url) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
		alertDialogBuilder.setMessage(clickedString)
				.setPositiveButton(R.string.open, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						boolean error = false;
						Intent intent = null;
						try {
							intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
						} catch (NullPointerException e) {
							error = true;
						}
						
						if (intent == null
								|| error
								|| !IntentChecker
										.isAvailable(
												mActivity,
												intent,
												new String[] { PackageManager.FEATURE_CAMERA })) {
							Crouton.makeText(
									mActivity,
									R.string.no_application_can_perform_this_action,
									ONStyle.ALERT).show();
							return;
						} else {		
						
							startActivity(intent);
						}
					}
				}).setNegativeButton(R.string.copy, new DialogInterface.OnClickListener() {
					@SuppressWarnings("deprecation")
					@Override
					public void onClick(DialogInterface dialog, int id) {
						// Creates a new text clip to put on the clipboard
						if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
						    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) mActivity.getSystemService(Activity.CLIPBOARD_SERVICE);
						    clipboard.setText("text to clip");
						} else {
						    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) mActivity.getSystemService(Activity.CLIPBOARD_SERVICE); 
						    android.content.ClipData clip = android.content.ClipData.newPlainText("text label", clickedString);
						    clipboard.setPrimaryClip(clip);
						}
						dialog.cancel();
					}
				});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}


	
	@SuppressLint("NewApi")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int x = (int) event.getX();
		int y = (int) event.getY();

		switch (event.getAction()) {
		
			case MotionEvent.ACTION_DOWN:
				Log.v(Constants.TAG, "MotionEvent.ACTION_DOWN");
				int w;
				
				Point displaySize = Display.getUsableSize(mActivity);
				w = displaySize.x;
				
				if (x < Constants.SWIPE_MARGIN || x > w - Constants.SWIPE_MARGIN) {
					swiping = true;
					startSwipeX = x;
				}
	
				break;
	
			case MotionEvent.ACTION_UP:
				Log.v(Constants.TAG, "MotionEvent.ACTION_UP");
				if (swiping)
					swiping = false;	
				break;
	
			case MotionEvent.ACTION_MOVE:
				if (swiping) {
					Log.v(Constants.TAG, "MotionEvent.ACTION_MOVE at position " + x + ", " + y);	
					if (Math.abs(x - startSwipeX) > Constants.SWIPE_OFFSET) {
						swiping = false;
						FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
						mActivity.animateTransition(transaction, mActivity.TRANSITION_VERTICAL);
						DetailFragment mDetailFragment = new DetailFragment();
						Bundle b = new Bundle();
						b.putParcelable(Constants.INTENT_NOTE, new Note());
						mDetailFragment.setArguments(b);
						transaction.replace(R.id.fragment_container, mDetailFragment, mActivity.FRAGMENT_DETAIL_TAG).addToBackStack(mActivity.FRAGMENT_DETAIL_TAG).commit();						
					}
				}
				break;
		}

		return true;
	}

	
	
	@Override
	public void onGlobalLayout() {
		
		int screenHeight = Display.getUsableSize(mActivity).y;
		int heightDiff = screenHeight - Display.getVisibleSize(root).y;
		// boolean keyboardVisible = heightDiff > screenHeight / 3;
		boolean keyboardVisible = heightDiff > 150;
		// boolean keyboardVisible = KeyboardUtils.isKeyboardShowed(title) || KeyboardUtils.isKeyboardShowed(content);
		
		if (keyboardVisible && keyboardPlaceholder == null) {
			shrinkLayouts(heightDiff);
			
		} else if (!keyboardVisible && keyboardPlaceholder != null) {
			restoreLayouts();
		}
	}
	
	
	
	private void shrinkLayouts(int heightDiff) {
		ViewGroup wrapper = ((ViewGroup)root.findViewById(R.id.detail_wrapper));
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
				&& !title.hasFocus()) {
			wrapper.removeView(titleCardView);
//			heightDiff -= Display.getActionbarHeight(mActivity) + Display.getStatusBarHeight(mActivity);
			heightDiff -= Display.getStatusBarHeight(mActivity);
			if (orientationChanged) {
				orientationChanged = false;
				heightDiff -= Display.getActionbarHeight(mActivity);
			}
		}
		wrapper.removeView(timestampsView);

		keyboardPlaceholder = new View(mActivity); 
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			root.addView(keyboardPlaceholder, LinearLayout.LayoutParams.MATCH_PARENT, heightDiff);
		}
	}
	
	private void restoreLayouts() {
		if (root != null) {
			ViewGroup wrapper = ((ViewGroup)root.findViewById(R.id.detail_wrapper));
			if (root.indexOfChild(keyboardPlaceholder) != -1) {
				root.removeView(keyboardPlaceholder);		
			}
			keyboardPlaceholder = null;
			if (wrapper.indexOfChild(titleCardView) == -1) {
				wrapper.addView(titleCardView, 0);
			}
			if (wrapper.indexOfChild(timestampsView) == -1) {
				wrapper.addView(timestampsView);
			}		
		}
	}

	
	
	
	@Override
	public void onAttachingFileErrorOccurred(Attachment mAttachment) {
		Crouton.makeText(mActivity, R.string.error_saving_attachments, ONStyle.ALERT).show();
		if (noteTmp.getAttachmentsList().contains(mAttachment)) {
			noteTmp.getAttachmentsList().remove(mAttachment);
			mAttachmentAdapter.notifyDataSetChanged();
			mGridView.autoresize();
		}
	}

	
	
	@Override
	public void onAttachingFileFinished(Attachment mAttachment) {
		noteTmp.getAttachmentsList().add(mAttachment);
		mAttachmentAdapter.notifyDataSetChanged();
		mGridView.autoresize();
	}


	@Override
	public void onReminderPicked(long reminder) {
		noteTmp.setAlarm(reminder);	
		if (mFragment.isAdded()) {
			datetime.setText(getString(R.string.alarm_set_on) + " " + DateHelper.getDateTimeShort(mActivity, reminder));
		}
	}

}



