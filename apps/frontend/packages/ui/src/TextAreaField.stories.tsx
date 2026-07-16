import type { Meta, StoryObj } from '@storybook/react-vite';
import { TextAreaField } from './TextAreaField';

const meta = {
  title: 'Primitives/TextAreaField',
  component: TextAreaField,
  args: { label: '본문', placeholder: '내용을 입력하세요' },
} satisfies Meta<typeof TextAreaField>;

export default meta;

type Story = StoryObj<typeof meta>;

// 단일 시각. 상태 매트릭스: 기본·입력됨·focus-visible·disabled·에러.
// hover 상태는 없다 — TextField와 같은 결정(텍스트 입력에 hover 어포던스 없음).
export const Playground: Story = {};

export const Filled: Story = {
  args: { defaultValue: '이번 주에는 워크벤치 게이트를 배선했다.\n다음 주에는 시각 회귀 도구를 검토한다.' },
};

export const FocusVisible: Story = { parameters: { pseudo: { focusVisible: true } } };

export const Disabled: Story = {
  args: { disabled: true, defaultValue: '수정할 수 없는 본문' },
};

export const Error: Story = {
  args: { error: '본문은 비어 있을 수 없습니다', defaultValue: '' },
};
