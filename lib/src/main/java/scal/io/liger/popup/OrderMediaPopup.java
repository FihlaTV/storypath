package scal.io.liger.popup;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.view.ActionMode;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.PopupWindow;

import java.util.List;

import scal.io.liger.R;
import scal.io.liger.adapter.OrderMediaAdapter;
import scal.io.liger.model.Card;
import scal.io.liger.model.StoryPath;
import scal.io.liger.view.ReorderableRecyclerView;


public class OrderMediaPopup {

    /**
     * Show a PopupWindow allowing you to re-order the clips. Assumes activity has an ActionBar
     * that will be used to present an ActionMode.
     *
     * TODO : When we migrate to Toolbar we'll need to pass a reference to it, as ActionMode will be set
     * on it, not the host Activity. Alternatively we might be able to set windowActionModeOverlay true:
     * http://stackoverflow.com/questions/26443403/toolbar-and-contextual-actionbar-with-appcompat-v7
     */
    public static void show(final Activity activity, final String medium, final List<Card> cards) {
        final View decorView = activity.getWindow().getDecorView();
        decorView.post(new Runnable() {
            @Override
            public void run() {
                // Create a PopupWindow that occupies the entire screen except the status and action bar
                final View popUpView = LayoutInflater.from(activity).inflate(R.layout.popup_order_media, (ViewGroup) decorView, false);
                ReorderableRecyclerView recyclerView = (ReorderableRecyclerView) popUpView.findViewById(R.id.recyclerView);
                recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                OrderMediaAdapter adapter = new OrderMediaAdapter(recyclerView, cards, medium);
                recyclerView.setReordableAdapter(adapter);

                Display display = activity.getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int width = size.x;
                int height = size.y;
                int actionBarHeight = activity.getActionBar().getHeight();

                Rect rectangle = new Rect();
                Window window = activity.getWindow();
                window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
                int statusBarHeight = rectangle.top;


                final PopupWindow popUp = new PopupWindow(popUpView, ViewGroup.LayoutParams.MATCH_PARENT, height - actionBarHeight - statusBarHeight, true);
                popUp.setFocusable(false);
                popUp.showAtLocation(decorView, Gravity.BOTTOM, 0, 0);

                final StoryPath storyPath = cards.get(0).getStoryPath();

                /** Callback from OrderMediaAdapter to handle clip re-order events */
                OrderMediaAdapter.OnReorderListener onReorderListener = new OrderMediaAdapter.OnReorderListener() {
                    @Override
                    public void onReorder(int firstIndex, int secondIndex) {
                        Card currentCard = cards.get(firstIndex);
                        int currentCardIndex = storyPath.getCardIndex(currentCard);
                        int newCardIndex = storyPath.getCardIndex(cards.get(secondIndex));
                        storyPath.swapCards(currentCardIndex, newCardIndex);
                    }
                };
                adapter.setOnReorderListener(onReorderListener);


                /** ActionMode Callback */
                ActionMode.Callback actionCallback = new ActionMode.Callback() {
                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        MenuInflater inflater = mode.getMenuInflater();
                        inflater.inflate(R.menu.order_media, menu);
                        mode.setTitle(activity.getString(R.string.clip_order));
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        if (item.getItemId() == R.id.menu_done) {
                            popUp.dismiss();
                            mode.finish();
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        popUp.dismiss();
                    }
                };
                activity.startActionMode(actionCallback);
            }
        });
    }
}
