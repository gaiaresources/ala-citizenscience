from datetime import datetime

def epoch_ms_to_datetime(ms_since_epoch):
    try:
        return datetime.fromtimestamp(ms_since_epoch/1000);
    except ValueError:
        # Some platforms such as Windows XP, do not handle negative ms_since_epoch values.
        return datetime.fromtimestamp(max(0,ms_since_epoch)/1000);