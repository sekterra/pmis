import { render, screen } from '@testing-library/react';
import React from 'react';
import { describe, expect, it } from 'vitest';
import ProvisioningStatus from './ProvisioningStatus';
import { formatCreateDatetime, provisioningLabel } from './rules';

describe('tenant list cells', () => {
  it('renders a known step, an unknown step, and an empty value', () => {
    const label = (code: string) => provisioningLabel(code, () => '실패');
    const { rerender } = render(
      <ProvisioningStatus code="FAILED" label={label} />,
    );
    expect(screen.getByText('실패')).toBeTruthy();
    rerender(<ProvisioningStatus code="MYSTERY" label={label} />);
    expect(screen.getByText('MYSTERY')).toBeTruthy();
    rerender(<ProvisioningStatus code={null} label={label} />);
    expect(screen.getByText('-')).toBeTruthy();
    expect(formatCreateDatetime(null)).toBe('-');
    expect(formatCreateDatetime('')).toBe('-');
  });
});
