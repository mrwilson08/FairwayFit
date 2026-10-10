// FairwayFit watchapp.
// Phone -> watch: a daily summary, or the current workout step.
// Watch -> phone: SELECT = main action, UP = +15 s rest.
//
// KEEP IN SYNC with PebbleProtocol.kt on the phone (PHASE_ and CMD_ values, keys, UUID).
#include <pebble.h>

#define PHASE_REPS          1
#define PHASE_HOLD_READY    2
#define PHASE_HOLD_RUNNING  3
#define PHASE_REST          4
#define PHASE_DONE          5

#define CMD_PRIMARY   1
#define CMD_ADD_REST  2

#define TYPE_WORKOUT  1
#define TYPE_SUMMARY  2

#define NO_SCORE (-1)
#define TEXT_LEN 40

static Window *s_window;
static TextLayer *s_title, *s_detail, *s_big, *s_hint;
static Layer *s_bar;

static char s_title_buf[TEXT_LEN], s_detail_buf[TEXT_LEN], s_big_buf[16], s_hint_buf[TEXT_LEN];
static int s_type = 0, s_phase = 0, s_amount = 0, s_progress = 0;

static void render(void) {
  text_layer_set_text(s_title, s_title_buf);
  text_layer_set_text(s_detail, s_detail_buf);

  if (s_type == TYPE_WORKOUT) {
    switch (s_phase) {
      case PHASE_REPS:
        snprintf(s_big_buf, sizeof(s_big_buf), "%d", s_amount);
        snprintf(s_hint_buf, sizeof(s_hint_buf), "reps  -  SELECT = done");
        break;
      case PHASE_HOLD_READY:
        snprintf(s_big_buf, sizeof(s_big_buf), "%d", s_amount);
        snprintf(s_hint_buf, sizeof(s_hint_buf), "sec  -  SELECT = start");
        break;
      case PHASE_HOLD_RUNNING:
        snprintf(s_big_buf, sizeof(s_big_buf), "%d", s_amount);
        snprintf(s_hint_buf, sizeof(s_hint_buf), "hold  -  SELECT = skip");
        break;
      case PHASE_REST:
        snprintf(s_big_buf, sizeof(s_big_buf), "%d", s_amount);
        snprintf(s_hint_buf, sizeof(s_hint_buf), "rest  -  UP +15s");
        break;
      case PHASE_DONE:
        snprintf(s_big_buf, sizeof(s_big_buf), "Done");
        snprintf(s_hint_buf, sizeof(s_hint_buf), "Nice work");
        break;
    }
  } else {
    // Summary: SCORE is the big number, WEEK is the hint (set when the message arrives).
    if (s_amount == NO_SCORE) snprintf(s_big_buf, sizeof(s_big_buf), "--");
    else snprintf(s_big_buf, sizeof(s_big_buf), "%d", s_amount);
  }
  text_layer_set_text(s_big, s_big_buf);
  text_layer_set_text(s_hint, s_hint_buf);
  layer_mark_dirty(s_bar);
}

static void bar_update(Layer *layer, GContext *ctx) {
  GRect b = layer_get_bounds(layer);
  graphics_context_set_fill_color(ctx, PBL_IF_COLOR_ELSE(GColorDarkGreen, GColorBlack));
  graphics_fill_rect(ctx, b, 0, GCornerNone);
  int w = s_type == TYPE_WORKOUT ? b.size.w * s_progress / 100 : 0;
  graphics_context_set_fill_color(ctx, PBL_IF_COLOR_ELSE(GColorSpringBud, GColorWhite));
  graphics_fill_rect(ctx, GRect(0, 0, w, b.size.h), 0, GCornerNone);
}

// Local 1-second countdown so the phone does not have to send every tick.
static void tick_handler(struct tm *t, TimeUnits units) {
  if (s_type != TYPE_WORKOUT) return;
  if (s_phase != PHASE_HOLD_RUNNING && s_phase != PHASE_REST) return;
  if (s_amount > 0) {
    s_amount--;
    if (s_amount == 0 && s_phase == PHASE_REST) vibes_short_pulse();
    render();
  }
}

static void inbox_received(DictionaryIterator *iter, void *context) {
  Tuple *t;
  if ((t = dict_find(iter, MESSAGE_KEY_TYPE)))  s_type = t->value->int32;
  if ((t = dict_find(iter, MESSAGE_KEY_TITLE))) snprintf(s_title_buf, sizeof(s_title_buf), "%s", t->value->cstring);
  if ((t = dict_find(iter, MESSAGE_KEY_DETAIL))) snprintf(s_detail_buf, sizeof(s_detail_buf), "%s", t->value->cstring);

  if (s_type == TYPE_WORKOUT) {
    int old_phase = s_phase;
    if ((t = dict_find(iter, MESSAGE_KEY_PHASE)))    s_phase = t->value->int32;
    if ((t = dict_find(iter, MESSAGE_KEY_AMOUNT)))   s_amount = t->value->int32;
    if ((t = dict_find(iter, MESSAGE_KEY_PROGRESS))) s_progress = t->value->int32;
    if (old_phase == PHASE_REST && s_phase != PHASE_REST) vibes_short_pulse();
    if (s_phase == PHASE_DONE) vibes_double_pulse();
  } else if (s_type == TYPE_SUMMARY) {
    if ((t = dict_find(iter, MESSAGE_KEY_SCORE))) s_amount = t->value->int32;
    if ((t = dict_find(iter, MESSAGE_KEY_WEEK)))  snprintf(s_hint_buf, sizeof(s_hint_buf), "%s", t->value->cstring);
    s_phase = 0;
    s_progress = 0;
  }
  render();
}

static void send_command(int command) {
  DictionaryIterator *out;
  if (app_message_outbox_begin(&out) != APP_MSG_OK) return;
  dict_write_int32(out, MESSAGE_KEY_COMMAND, command);
  app_message_outbox_send();
}

static void select_click(ClickRecognizerRef r, void *ctx) { if (s_type == TYPE_WORKOUT) send_command(CMD_PRIMARY); }
static void up_click(ClickRecognizerRef r, void *ctx)     { if (s_type == TYPE_WORKOUT && s_phase == PHASE_REST) send_command(CMD_ADD_REST); }

static void click_config(void *ctx) {
  window_single_click_subscribe(BUTTON_ID_SELECT, select_click);
  window_single_click_subscribe(BUTTON_ID_UP, up_click);
}

static TextLayer *make_text(Layer *root, GRect frame, const char *font, GTextAlignment align, GColor color) {
  TextLayer *l = text_layer_create(frame);
  text_layer_set_background_color(l, GColorClear);
  text_layer_set_text_color(l, color);
  text_layer_set_font(l, fonts_get_system_font(font));
  text_layer_set_text_alignment(l, align);
  layer_add_child(root, text_layer_get_layer(l));
  return l;
}

static void window_load(Window *w) {
  Layer *root = window_get_root_layer(w);
  GRect b = layer_get_bounds(root);
  int pad = PBL_IF_ROUND_ELSE(18, 6);
  int width = b.size.w - 2 * pad;
  GColor accent = PBL_IF_COLOR_ELSE(GColorSpringBud, GColorWhite);
  GColor body = PBL_IF_COLOR_ELSE(GColorWhite, GColorWhite);
  GColor muted = PBL_IF_COLOR_ELSE(GColorLightGray, GColorWhite);

  window_set_background_color(w, PBL_IF_COLOR_ELSE(GColorBlack, GColorBlack));

  s_title  = make_text(root, GRect(pad, PBL_IF_ROUND_ELSE(14, 4), width, 44), FONT_KEY_GOTHIC_24_BOLD, PBL_IF_ROUND_ELSE(GTextAlignmentCenter, GTextAlignmentLeft), body);
  s_detail = make_text(root, GRect(pad, PBL_IF_ROUND_ELSE(58, 48), width, 22), FONT_KEY_GOTHIC_18, PBL_IF_ROUND_ELSE(GTextAlignmentCenter, GTextAlignmentLeft), muted);
  s_big    = make_text(root, GRect(pad, PBL_IF_ROUND_ELSE(76, 66), width, 50), FONT_KEY_BITHAM_42_BOLD, GTextAlignmentCenter, accent);
  s_hint   = make_text(root, GRect(pad, b.size.h - PBL_IF_ROUND_ELSE(50, 40), width, 22), FONT_KEY_GOTHIC_14, GTextAlignmentCenter, muted);
  text_layer_set_overflow_mode(s_title, GTextOverflowModeTrailingEllipsis);

  s_bar = layer_create(GRect(0, b.size.h - 6, b.size.w, 6));
  layer_set_update_proc(s_bar, bar_update);
  layer_add_child(root, s_bar);

  snprintf(s_title_buf, sizeof(s_title_buf), "FairwayFit");
  snprintf(s_detail_buf, sizeof(s_detail_buf), "Waiting for phone...");
  snprintf(s_big_buf, sizeof(s_big_buf), "--");
  snprintf(s_hint_buf, sizeof(s_hint_buf), "Open FairwayFit on phone");
  text_layer_set_text(s_title, s_title_buf);
  text_layer_set_text(s_detail, s_detail_buf);
  text_layer_set_text(s_big, s_big_buf);
  text_layer_set_text(s_hint, s_hint_buf);
}

static void window_unload(Window *w) {
  text_layer_destroy(s_title);
  text_layer_destroy(s_detail);
  text_layer_destroy(s_big);
  text_layer_destroy(s_hint);
  layer_destroy(s_bar);
}

static void init(void) {
  s_window = window_create();
  window_set_click_config_provider(s_window, click_config);
  window_set_window_handlers(s_window, (WindowHandlers){ .load = window_load, .unload = window_unload });
  window_stack_push(s_window, true);

  app_message_register_inbox_received(inbox_received);
  app_message_open(256, 64);
  tick_timer_service_subscribe(SECOND_UNIT, tick_handler);
}

static void deinit(void) {
  tick_timer_service_unsubscribe();
  window_destroy(s_window);
}

int main(void) {
  init();
  app_event_loop();
  deinit();
}
