import { fireEvent, render, screen } from '@testing-library/react';
import { Form, Input } from 'antd';
import React from 'react';
import { describe, expect, it } from 'vitest';
import { toFormFields } from '@/core/request/errors';

function Host() {
  const [form] = Form.useForm();
  return (
    <Form form={form}>
      <Form.Item name="tenantCode" label="코드">
        <Input />
      </Form.Item>
      <button
        type="button"
        onClick={() => {
          form.setFields(
            toFormFields([
              { field: 'tenantCode', message: '코드가 올바르지 않습니다.' },
            ]) as Parameters<typeof form.setFields>[0],
          );
        }}
      >
        apply
      </button>
    </Form>
  );
}

describe('create form field errors', () => {
  it('marks the matching input with the server field error', () => {
    render(<Host />);
    fireEvent.click(screen.getByRole('button', { name: 'apply' }));
    const input = screen.getByLabelText('코드');
    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(input.getAttribute('aria-describedby')).toBe('tenantCode_help');
  });
});
