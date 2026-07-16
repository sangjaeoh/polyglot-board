import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button, buttonClass, type ButtonVariant } from './Button';

const VARIANTS: ButtonVariant[] = ['primary', 'secondary', 'danger'];

const meta = {
  title: 'Primitives/Button',
  component: Button,
  argTypes: {
    variant: { control: 'inline-radio', options: VARIANTS },
    disabled: { control: 'boolean' },
  },
  args: { variant: 'primary', children: '버튼 라벨' },
} satisfies Meta<typeof Button>;

export default meta;

type Story = StoryObj<typeof meta>;

// variant 전수를 한 행에 렌더한다 — 상태 스토리가 공유하는 매트릭스 기준 행.
function VariantRow({ disabled = false }: { disabled?: boolean }) {
  return (
    <div className="flex flex-wrap items-center gap-4">
      {VARIANTS.map((variant) => (
        <Button key={variant} variant={variant} disabled={disabled}>
          {variant}
        </Button>
      ))}
    </div>
  );
}

// 버튼 시각 링크(item 4 승격 buttonClass): <button>이 아닌 시맨틱 요소가 같은 스킨을 소비한다.
function LinkRow() {
  return (
    <div className="flex flex-wrap items-center gap-4">
      {VARIANTS.map((variant) => (
        <a key={variant} href="#link" className={buttonClass(variant)}>
          {variant} 링크
        </a>
      ))}
    </div>
  );
}

// controls로 variant·disabled를 조작하는 단일 버튼.
export const Playground: Story = {};

// 기본 상태 — 전 variant.
export const Variants: Story = { render: () => <VariantRow /> };

// 상태: hover·focus-visible·active·disabled. hover/focus/active는 pseudo-states 애드온이 강제한다.
export const Hover: Story = {
  render: () => <VariantRow />,
  parameters: { pseudo: { hover: true } },
};

export const FocusVisible: Story = {
  render: () => <VariantRow />,
  parameters: { pseudo: { focusVisible: true } },
};

export const Active: Story = {
  render: () => <VariantRow />,
  parameters: { pseudo: { active: true } },
};

export const Disabled: Story = { render: () => <VariantRow disabled /> };

// 버튼 시각 링크 — 기본·hover·focus-visible.
export const AsLink: Story = { render: () => <LinkRow /> };

export const AsLinkHover: Story = {
  render: () => <LinkRow />,
  parameters: { pseudo: { hover: true } },
};

export const AsLinkFocusVisible: Story = {
  render: () => <LinkRow />,
  parameters: { pseudo: { focusVisible: true } },
};
