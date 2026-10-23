import type { LucideIcon } from 'lucide-react';
import { Construction } from 'lucide-react';
import { PageHeader } from '../ui/PageHeader';
import { EmptyState } from '../ui/EmptyState';
import { StatusBadge } from '../ui/StatusBadge';

interface PlaceholderPageProps {
    title: string;
    description: string;
    feature: string;
    icon?: LucideIcon;
}

export default function PlaceholderPage({ title, description, feature, icon: Icon = Construction }: PlaceholderPageProps) {
    return (
        <div className="app-page">
            <PageHeader title={title} description={description} action={<StatusBadge>{feature}</StatusBadge>} />
            <EmptyState icon={<Icon aria-hidden="true" className="h-8 w-8" />} title={`${title} is next in the build plan`} description="The navigation and page foundation are ready. This workflow will be connected end to end in its dedicated feature increment." />
        </div>
    );
}
