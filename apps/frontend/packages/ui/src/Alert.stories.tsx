import type { Meta, StoryObj } from '@storybook/react-vite';
import { Alert } from './Alert';

const meta = {
  title: 'Primitives/Alert',
  component: Alert,
  args: { children: '무언가 잘못되었습니다. 다시 시도해 주세요.' },
} satisfies Meta<typeof Alert>;

export default meta;

type Story = StoryObj<typeof meta>;

// Alert는 단일 시각(위험)이고 인터랙션 상태가 없는 정적 표시다(role="alert").
// 상태 매트릭스는 콘텐츠 길이 변주로 충분하다 — 짧은 한 줄과 줄바꿈되는 긴 문장.
export const Default: Story = {};

export const LongContent: Story = {
  args: {
    children:
      '입력을 확인해 주세요. 제목은 비어 있을 수 없고 200자를 넘을 수 없으며, 본문 또한 필수 항목입니다.',
  },
};
