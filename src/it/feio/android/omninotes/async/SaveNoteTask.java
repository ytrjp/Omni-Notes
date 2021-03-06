package it.feio.android.omninotes.async;

import it.feio.android.omninotes.DetailFragment;
import it.feio.android.omninotes.R;
import it.feio.android.omninotes.db.DbHelper;
import it.feio.android.omninotes.models.Attachment;
import it.feio.android.omninotes.models.Note;
import it.feio.android.omninotes.receiver.AlarmReceiver;
import it.feio.android.omninotes.utils.Constants;
import it.feio.android.omninotes.utils.FileHelper;
import it.feio.android.omninotes.utils.StorageManager;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class SaveNoteTask extends AsyncTask<Note, Void, Note> {
	private final WeakReference<DetailFragment> mDetailFragmentReference;
	private final Activity mActivity;
	private boolean error = false;
	private boolean updateLastModification = true;

	public SaveNoteTask(DetailFragment activity, boolean updateLastModification) {
		super();
		mDetailFragmentReference = new WeakReference<DetailFragment>(activity);
		mActivity = activity.getActivity();
		this.updateLastModification = updateLastModification;
	}

	@Override
	protected Note doInBackground(Note... params) {
		Note note = params[0];
		createAttachmentCopy(note);
		purgeRemovedAttachments(note);
		
		if (!error) {
			DbHelper db = new DbHelper(mActivity);		
			// Note updating on database
			note = db.updateNote(note, updateLastModification);
		} else {
			Toast.makeText(mActivity, mActivity.getString(R.string.error_saving_attachments), Toast.LENGTH_SHORT).show();
		}
			
		return note;
	}
	
	private void purgeRemovedAttachments(Note note) {
		ArrayList<Attachment> deletedAttachments = note.getAttachmentsListOld();
		for (Attachment attachment : note.getAttachmentsList()) {
			if (attachment.getId() != 0) {
				deletedAttachments.remove(attachment);
			}
		}
		// Remove from database deleted attachments
		for (Attachment deletedAttachment : deletedAttachments) {
			StorageManager.delete(mActivity, deletedAttachment.getUri().getPath());
		}
	}

	@Override
	protected void onPostExecute(Note note) {
		super.onPostExecute(note);
		
		// Set reminder if is not passed yet
		long now = Calendar.getInstance().getTimeInMillis();
		if (note.getAlarm() != null && Long.parseLong(note.getAlarm()) >= now) {
			setAlarm(note);
		}

		// Return back to parent activity now that the heavy work is done to speed up interface
		if (isAlive(mDetailFragmentReference)) {
			mDetailFragmentReference.get().goHome();
		}
	}
	
	
	/**
	 * Cheks if activity is still alive and not finishing
	 * @param weakDetailFragmentReference
	 * @return True or false
	 */
	private boolean isAlive(WeakReference<DetailFragment> weakDetailFragmentReference) {
		if (weakDetailFragmentReference == null
				|| weakDetailFragmentReference.get() == null
				|| !weakDetailFragmentReference.get().isAdded()
				|| weakDetailFragmentReference.get().getActivity()
						.isFinishing()) {
			return false;
		}
		return true;
	}

	
	/**
	 * Makes copies of attachments files and replace uris 
	 * @param note
	 */
	private void createAttachmentCopy(Note note) {
		File destination;
		Uri uri;
		
		for (Attachment attachment : note.getAttachmentsList()) {
			
			uri = attachment.getUri();
//			uri = Uri.parse(FileHelper.getPath(mActivity, attachment.getUri()));
			
			if (uri == null) {
				error = true;
				return;
			}
				
			
			// The copy will be made only if it's a new attachment or if attachment directory is not yet the destination one
//			if (attachment.getId() != 0 || 
//					uri.getPath().contains(mActivity.getExternalFilesDir(null).getAbsolutePath())) {
			if (!attachment.getMoveWhenNoteSaved()) {
				break;
			}
			
			// Avoids trying to save attachment if fragment has been removed
			if(!isAlive(mDetailFragmentReference)) continue;
			
			String extension = "";
			if (attachment.getMime_type().equals(Constants.MIME_TYPE_AUDIO))
				extension = Constants.MIME_TYPE_AUDIO_EXT;
			else if (attachment.getMime_type().equals(Constants.MIME_TYPE_IMAGE))
				extension = Constants.MIME_TYPE_IMAGE_EXT;
			else if (attachment.getMime_type().equals(Constants.MIME_TYPE_SKETCH))
				extension = Constants.MIME_TYPE_SKETCH_EXT;
			else if (attachment.getMime_type().equals(Constants.MIME_TYPE_VIDEO))
				extension = Constants.MIME_TYPE_VIDEO_EXT;
			else if (attachment.getMime_type().equals(Constants.MIME_TYPE_FILES)) {
//				extension = StorageManager.getMimeType(mActivity, attachment.getUri());
//			extension = StorageManager.getRealPathFromURI(mActivity, attachment.getUri());
				String path = FileHelper.getPath(mActivity, uri);
				if (path != null) {
//					extension = path.substring(path.lastIndexOf("."), path.length());
					extension = FileHelper.getFileExtension(path);
				}
			}				
				
			destination = StorageManager.createExternalStoragePrivateFile(mActivity, uri, extension);
			Log.v(Constants.TAG, "Moving attachment " + uri + " to " + destination);
			
			if (destination == null) {				
				Log.e(Constants.TAG, "Can't find or move file");
				break;
			}
			
			// Replace uri
			attachment.setUri(Uri.fromFile(destination));
			
			// Gingerbread don't allow creating custom uri to store video recordings
			// so they're stored in default location. Once the copy is done original can be deleted
			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1
					&& attachment.getMime_type().equals(Constants.MIME_TYPE_VIDEO)) {
				StorageManager.delete(mActivity, uri.getPath());
			}
		}
	}
	


	private void setAlarm(Note note) {
		Intent intent = new Intent(mActivity, AlarmReceiver.class);
		intent.putExtra(Constants.INTENT_NOTE, note);
		PendingIntent sender = PendingIntent.getBroadcast(mActivity, Constants.INTENT_ALARM_CODE, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager am = (AlarmManager) mActivity.getSystemService(Activity.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, Long.parseLong(note.getAlarm()), sender);
	}
	
	
	
	
	
}
