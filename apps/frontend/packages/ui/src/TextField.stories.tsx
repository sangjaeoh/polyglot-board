import type { Meta, StoryObj } from '@storybook/react-vite';
import { TextField } from './TextField';

const meta = {
  title: 'Primitives/TextField',
  component: TextField,
  args: { label: '제목', placeholder: '제목을 입력하세요' },
} satisfies Meta<typeof TextField>;

export default meta;

type Story = StoryObj<typeof meta>;

// 단일 시각. 상태 매트릭스: 기본·입력됨·focus-visible·disabled·에러.
// hover 상태는 없다 — 텍스트 입력에 hover 어포던스를 두지 않는 현 디자인 결정(투기 금지).
export const Playground: Story = {};

export const Filled: Story = { args: { defaultValue: '주간 회고' } };

export const FocusVisible: Story = { parameters: { pseudo: { focusVisible: true } } };

export const Disabled: Story = { args: { disabled: true, defaultValue: '수정할 수 없는 값' } };

export const Error: Story = {
  args: { error: '제목은 비어 있을 수 없습니다', defaultValue: '' },
};
