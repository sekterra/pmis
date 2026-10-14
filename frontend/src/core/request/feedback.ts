import type { Feedback } from './errors';

const noop = () => undefined;

let feedback: Feedback = {
  warning: noop,
  error: noop,
  notify: noop,
};

export function bindFeedback(next: Feedback): void {
  feedback = next;
}

export function getFeedback(): Feedback {
  return feedback;
}
