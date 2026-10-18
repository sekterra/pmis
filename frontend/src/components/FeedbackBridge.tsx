import { App } from 'antd';
import React, { useEffect } from 'react';
import { bindFeedback } from '@/core/request/feedback';

const FeedbackBridge: React.FC = () => {
  const { message, notification } = App.useApp();
  useEffect(() => {
    bindFeedback({
      warning: (text) => {
        message.warning(text);
      },
      error: (text) => {
        message.error(text);
      },
      notify: (title, description) => {
        notification.open({ title, description });
      },
    });
  }, [message, notification]);
  return null;
};

export default FeedbackBridge;
